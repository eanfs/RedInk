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

	"github.com/cloudwego/hertz/pkg/common/hlog"
	openai "github.com/openai/openai-go"
	"github.com/openai/openai-go/option"
)

type OpenAIGenerator struct {
	config map[string]interface{}
	apiKey string
	baseUrl string
	client *openai.Client
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
		config: config,
		apiKey: apiKey,
		baseUrl: baseUrl,
		client: &client,
		httpClient: &http.Client{Timeout: 300 * time.Second},
	}, nil
}

func (g *OpenAIGenerator) ValidateConfig() bool {
	return g.apiKey != ""
}

func (g *OpenAIGenerator) GenerateText(ctx context.Context, prompt string, opts ...TextOption) (string, error) {
	options := &TextOptions{
		Model: "gpt-3.5-turbo",
		Temperature: 0.7,
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
        Messages: messages,
        Model: openai.ChatModel(options.Model),
        Temperature: openai.Float(options.Temperature),
        MaxTokens: openai.Int(int64(options.MaxOutputTokens)),
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
    
    endpointType := "/v1/images/generations"
    if val, ok := g.config["endpoint_type"].(string); ok && val != "" {
        endpointType = val
    }
    if endpointType == "images" { endpointType = "/v1/images/generations" }
    if endpointType == "chat" { endpointType = "/v1/chat/completions" }

    if strings.Contains(endpointType, "chat") || strings.Contains(endpointType, "completions") {
        return g.generateViaChat(ctx, prompt, model)
    }
    return g.generateViaSDKImage(ctx, prompt, options.Size, model)
}

func (g *OpenAIGenerator) generateViaSDKImage(ctx context.Context, prompt, size, model string) ([]byte, error) {
    resp, err := g.client.Images.Generate(ctx, openai.ImageGenerateParams{
        Model: openai.ImageModel(model),
        Prompt: prompt, // Direct string
        N: openai.Int(1),
        ResponseFormat: openai.ImageGenerateParamsResponseFormatB64JSON,
        Size: openai.ImageGenerateParamsSize(size),
    })

    if err != nil {
        return nil, fmt.Errorf("failed to generate image: %w", err)
    }

    if len(resp.Data) == 0 {
        return nil, fmt.Errorf("no image data in response")
    }

    if resp.Data[0].B64JSON != "" {
        return base64.StdEncoding.DecodeString(resp.Data[0].B64JSON)
    }
    
    if resp.Data[0].URL != "" {
        return g.downloadImage(resp.Data[0].URL)
    }

    return nil, fmt.Errorf("no image data found in response")
}

func (g *OpenAIGenerator) generateViaChat(ctx context.Context, prompt, model string) ([]byte, error) {
    resp, err := g.client.Chat.Completions.New(ctx, openai.ChatCompletionNewParams{
        Messages: []openai.ChatCompletionMessageParamUnion{
            openai.UserMessage(prompt),
        },
        Model: openai.ChatModel(model),
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
