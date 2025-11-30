package generators

import (
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"regexp"
	"strings"
	"time"

	"bytes"
	"encoding/json"
	"image"
	"image/jpeg"
	_ "image/png"

	"github.com/cloudwego/hertz/pkg/common/hlog"
	openai "github.com/openai/openai-go"
	"github.com/openai/openai-go/option"
)

type OpenAIGenerator struct {
	config     map[string]interface{}
	apiKey     string
	baseUrl    string
	client     *openai.Client
	httpClient *http.Client
}

func NewOpenAIGenerator(config map[string]interface{}) (*OpenAIGenerator, error) {
	apiKey, _ := config["api_key"].(string)
	baseUrl, _ := config["base_url"].(string)

	if apiKey == "" {
		return nil, fmt.Errorf("OpenAI API Key is required")
	}

	opts := []option.RequestOption{
		option.WithAPIKey(apiKey),
	}
	if baseUrl != "" {
		opts = append(opts, option.WithBaseURL(baseUrl))
	}

	client := openai.NewClient(opts...)

	return &OpenAIGenerator{
		config:     config,
		apiKey:     apiKey,
		baseUrl:    baseUrl,
		client:     &client,
		httpClient: &http.Client{Timeout: 300 * time.Second},
	}, nil
}

func (g *OpenAIGenerator) ValidateConfig() bool {
	return g.apiKey != ""
}

func (g *OpenAIGenerator) GenerateText(ctx context.Context, prompt string, opts ...TextOption) (string, error) {
	options := &TextOptions{
		Model:           "gpt-3.5-turbo",
		Temperature:     0.7,
		MaxOutputTokens: 2000,
	}
	for _, opt := range opts {
		opt(options)
	}

	if val, ok := g.config["model"].(string); ok && val != "" {
		options.Model = val
	}

	messages := []openai.ChatCompletionMessageParamUnion{}

	// System prompt
	if options.SystemPrompt != "" {
		messages = append(messages, openai.SystemMessage(options.SystemPrompt))
	}

	// User message with potential images
	if len(options.Images) > 0 {
		parts := []openai.ChatCompletionContentPartUnionParam{
			openai.TextContentPart(prompt),
		}
		for _, img := range options.Images {
			var imageUrl string
			if strings.HasPrefix(img, "http") || strings.HasPrefix(img, "data:") {
				imageUrl = img
			} else {
				imageUrl = "data:image/jpeg;base64," + img
			}
			parts = append(parts, openai.ImageContentPart(
				openai.ChatCompletionContentPartImageImageURLParam{
					URL: imageUrl,
				},
			))
		}
		messages = append(messages, openai.UserMessage(parts))
	} else {
		messages = append(messages, openai.UserMessage(prompt))
	}

	resp, err := g.client.Chat.Completions.New(ctx, openai.ChatCompletionNewParams{
		Messages:    messages,
		Model:       openai.ChatModel(options.Model),
		Temperature: openai.Float(options.Temperature),
		MaxTokens:   openai.Int(int64(options.MaxOutputTokens)),
	})

	if err != nil {
		return "", fmt.Errorf("failed to create chat completion: %w", err)
	}

	if len(resp.Choices) == 0 {
		return "", fmt.Errorf("no choices in response")
	}

	return resp.Choices[0].Message.Content, nil
}

func (g *OpenAIGenerator) GenerateImage(ctx context.Context, prompt string, opts ...ImageOption) ([]byte, error) {
	options := &ImageOptions{
		Size: "1024x1024",
	}
	for _, opt := range opts {
		opt(options)
	}

	model := "dall-e-3"
	if val, ok := g.config["model"].(string); ok && val != "" {
		model = val
	}
	if options.Model != "" {
		model = options.Model
	}

	endpointType := "/v1/images/generations"
	if val, ok := g.config["endpoint_type"].(string); ok && val != "" {
		endpointType = val
	}
	if endpointType == "images" {
		endpointType = "/v1/images/generations"
	}
	if endpointType == "chat" {
		endpointType = "/v1/chat/completions"
	}

	var lastErr error
	maxRetries := 3
	for i := 0; i < maxRetries; i++ {
		var result []byte
		var err error

		if strings.Contains(endpointType, "chat") || strings.Contains(endpointType, "completions") {
			result, err = g.generateViaChat(ctx, prompt, options, model)
		} else {
			result, err = g.generateViaImagesAPI(ctx, prompt, options, model)
		}

		if err == nil {
			return result, nil
		}

		lastErr = err
		hlog.Warnf("Image generation failed (attempt %d/%d): %v", i+1, maxRetries, err)

		if i < maxRetries-1 {
			// Exponential backoff with jitter could be better, but simple exponential is fine
			time.Sleep(time.Duration(2*(1<<i)) * time.Second)
		}
	}
	return nil, lastErr
}

func (g *OpenAIGenerator) generateViaImagesAPI(ctx context.Context, prompt string, options *ImageOptions, model string) ([]byte, error) {
	// Construct payload
	payload := map[string]interface{}{
		"model":           model,
		"prompt":          prompt,
		"response_format": "b64_json",
		"aspect_ratio":    options.AspectRatio,
		"image_size":      options.Size,
	}
	if payload["aspect_ratio"] == "" {
		payload["aspect_ratio"] = "3:4" // Default
	}
	if payload["image_size"] == "" {
		payload["image_size"] = "4K" // Default
	}

	// Handle reference images
	var allRefImages [][]byte
	if len(options.ReferenceImages) > 0 {
		allRefImages = append(allRefImages, options.ReferenceImages...)
	}
	if len(options.ReferenceImage) > 0 {
		// Check if already in list to avoid duplicates (simple check)
		found := false
		for _, img := range allRefImages {
			if bytes.Equal(img, options.ReferenceImage) {
				found = true
				break
			}
		}
		if !found {
			allRefImages = append(allRefImages, options.ReferenceImage)
		}
	}

	if len(allRefImages) > 0 {
		hlog.Debugf("Adding %d reference images", len(allRefImages))
		var imageUris []string
		for _, imgData := range allRefImages {
			compressed := g.compressImage(imgData, 200)
			b64 := base64.StdEncoding.EncodeToString(compressed)
			imageUris = append(imageUris, "data:image/png;base64,"+b64)
		}
		payload["image"] = imageUris

		// Enhanced prompt logic
		enhancedPrompt := fmt.Sprintf(`参考提供的 %d 张图片的风格（色彩、光影、构图、氛围），生成一张新图片。

新图片内容：%s

要求：
1. 保持相似的色调和氛围
2. 使用相似的光影处理
3. 保持一致的画面质感
4. 如果参考图中有人物或产品，可以适当融入`, len(allRefImages), prompt)
		payload["prompt"] = enhancedPrompt
	}

	url := g.baseUrl + "/v1/images/generations"
	if val, ok := g.config["endpoint_type"].(string); ok && val != "" && strings.HasPrefix(val, "/") {
		url = g.baseUrl + val
	}

	hlog.Debugf("Sending request to: %s", url)

	jsonBody, err := json.Marshal(payload)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal payload: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewBuffer(jsonBody))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+g.apiKey)
	req.Header.Set("Content-Type", "application/json")

	resp, err := g.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		bodyBytes, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("API request failed (status %d): %s", resp.StatusCode, string(bodyBytes))
	}

	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	if data, ok := result["data"].([]interface{}); ok && len(data) > 0 {
		item := data[0].(map[string]interface{})
		if b64Json, ok := item["b64_json"].(string); ok {
			if strings.HasPrefix(b64Json, "data:") {
				parts := strings.Split(b64Json, ",")
				if len(parts) > 1 {
					return base64.StdEncoding.DecodeString(parts[1])
				}
			}
			return base64.StdEncoding.DecodeString(b64Json)
		}
		// Handle URL if needed, though Python code prioritizes b64_json
	}

	return nil, fmt.Errorf("failed to extract image data from response")
}

func (g *OpenAIGenerator) compressImage(data []byte, maxSizeKB int) []byte {
	if len(data) <= maxSizeKB*1024 {
		return data
	}
	img, _, err := image.Decode(bytes.NewReader(data))
	if err != nil {
		return data
	}
	var buf bytes.Buffer
	jpeg.Encode(&buf, img, &jpeg.Options{Quality: 80})
	if buf.Len() <= maxSizeKB*1024 {
		return buf.Bytes()
	}
	buf.Reset()
	jpeg.Encode(&buf, img, &jpeg.Options{Quality: 60})
	return buf.Bytes()
}

func (g *OpenAIGenerator) generateViaChat(ctx context.Context, prompt string, options *ImageOptions, model string) ([]byte, error) {
	messages := []openai.ChatCompletionMessageParamUnion{}

	userContent := []openai.ChatCompletionContentPartUnionParam{
		openai.TextContentPart(prompt),
	}

	// Handle reference images
	var allRefImages [][]byte
	if len(options.ReferenceImages) > 0 {
		allRefImages = append(allRefImages, options.ReferenceImages...)
	}
	if len(options.ReferenceImage) > 0 {
		found := false
		for _, img := range allRefImages {
			if bytes.Equal(img, options.ReferenceImage) {
				found = true
				break
			}
		}
		if !found {
			allRefImages = append(allRefImages, options.ReferenceImage)
		}
	}

	for _, imgData := range allRefImages {
		compressed := g.compressImage(imgData, 200)
		b64 := base64.StdEncoding.EncodeToString(compressed)
		userContent = append(userContent, openai.ImageContentPart(
			openai.ChatCompletionContentPartImageImageURLParam{
				URL: "data:image/png;base64," + b64,
			},
		))
	}

	messages = append(messages, openai.UserMessage(userContent))

	resp, err := g.client.Chat.Completions.New(ctx, openai.ChatCompletionNewParams{
		Messages:    messages,
		Model:       openai.ChatModel(model),
		Temperature: openai.Float(1.0),
	})

	if err != nil {
		return nil, fmt.Errorf("failed to create chat completion for image: %w", err)
	}

	if len(resp.Choices) == 0 {
		return nil, fmt.Errorf("no choices in response")
	}

	content := resp.Choices[0].Message.Content

	re := regexp.MustCompile(`!\[.*?\]\((https?://[^\s\)]+)\)`)
	matches := re.FindStringSubmatch(content)
	if len(matches) > 1 {
		return g.downloadImage(matches[1])
	}

	if strings.HasPrefix(content, "data:image") {
		parts := strings.Split(content, ",")
		if len(parts) > 1 {
			return base64.StdEncoding.DecodeString(parts[1])
		}
	}

	if strings.HasPrefix(content, "http") {
		return g.downloadImage(strings.TrimSpace(content))
	}

	return nil, fmt.Errorf("could not extract image from chat response: %s", content)
}

func (g *OpenAIGenerator) downloadImage(url string) ([]byte, error) {
	hlog.Infof("Downloading image from %s", url)
	resp, err := g.httpClient.Get(url)
	if err != nil {
		return nil, fmt.Errorf("failed to download image: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to download image: %d %s", resp.StatusCode, resp.Status)
	}
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read downloaded image body: %w", err)
	}
	return body, nil
}
