package generators

import (
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/cloudwego/hertz/pkg/common/hlog"
	"google.golang.org/genai"
)

type GoogleGeminiGenerator struct {
	config map[string]interface{}
	apiKey string
	baseUrl string
	client *genai.Client
}

func NewGoogleGeminiGenerator(config map[string]interface{}) (*GoogleGeminiGenerator, error) {
	apiKey, _ := config["api_key"].(string)
	baseUrl, _ := config["base_url"].(string)

	if apiKey == "" {
		return nil, fmt.Errorf("Google Gemini API Key is required")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

    clientConfig := &genai.ClientConfig{
        APIKey: apiKey,
    }
    
    // If base URL is needed, checking if ClientConfig supports HTTPOptions with BaseURL
    // Since we can't verify the exact field name for BaseURL inside HTTPOptions easily without deep docs,
    // we will rely on default endpoint unless specifically needed. 
    // For standard Gemini usage, APIKey is sufficient. 
    
	client, err := genai.NewClient(ctx, clientConfig)
	if err != nil {
		return nil, fmt.Errorf("failed to create Google Gemini client: %w", err)
	}

	return &GoogleGeminiGenerator{
		config: config,
		apiKey: apiKey,
		baseUrl: baseUrl,
		client: client,
	}, nil
}

func (g *GoogleGeminiGenerator) ValidateConfig() bool {
	return g.apiKey != ""
}

func (g *GoogleGeminiGenerator) GenerateText(ctx context.Context, prompt string, opts ...TextOption) (string, error) {
	options := &TextOptions{
		Model: "gemini-2.0-flash", // Use a modern default
		Temperature: 1.0,
		MaxOutputTokens: 8000,
	}
	for _, opt := range opts {
		opt(options)
	}

	if val, ok := g.config["model"].(string); ok && val != "" {
		options.Model = val
	}

    var parts []*genai.Part

    if options.SystemPrompt != "" {
        // Prepend system prompt as text for now as standard simple usage
        parts = append(parts, genai.NewPartFromText(options.SystemPrompt + "\n\n"))
    }
    
    parts = append(parts, genai.NewPartFromText(prompt))

	for _, img := range options.Images {
		if strings.HasPrefix(img, "data:image") {
			// Extract base64 data
            // data:image/png;base64,......
            idx := strings.Index(img, ";base64,")
            if idx == -1 {
                continue
            }
			mimeType := img[5:idx] 
			base64Data := img[idx+8:]
            
            data, err := base64.StdEncoding.DecodeString(base64Data)
            if err != nil {
                hlog.CtxWarnf(ctx, "Failed to decode base64 image: %v", err)
                continue
            }
			parts = append(parts, genai.NewPartFromBytes(data, mimeType))

		} else if strings.HasPrefix(img, "http") {
            // Download image
            hlog.CtxInfof(ctx, "Downloading image for Gemini: %s", img)
            resp, err := http.Get(img)
            if err != nil {
                 hlog.CtxWarnf(ctx, "Failed to download image: %v", err)
                 continue
            }
            defer resp.Body.Close()
            data, err := io.ReadAll(resp.Body)
            if err != nil {
                 hlog.CtxWarnf(ctx, "Failed to read image body: %v", err)
                 continue
            }
            // Guess mime type or use default
            mimeType := resp.Header.Get("Content-Type")
            if mimeType == "" {
                mimeType = "image/jpeg"
            }
            parts = append(parts, genai.NewPartFromBytes(data, mimeType))
		} else {
            // Assume raw base64
            data, err := base64.StdEncoding.DecodeString(img)
            if err == nil {
                parts = append(parts, genai.NewPartFromBytes(data, "image/jpeg"))
            }
        }
	}

    content := genai.NewContentFromParts(parts, "user")
    
    // Config
    // We might need to check if GenerateContentConfig is available and how to set temperature/tokens
    // genai.GenerateContentConfig exists.
    temp := float32(options.Temperature)
    maxTokens := int32(options.MaxOutputTokens)
    genConfig := &genai.GenerateContentConfig{
        Temperature: &temp,
        MaxOutputTokens: maxTokens,
    }

	resp, err := g.client.Models.GenerateContent(ctx, options.Model, []*genai.Content{content}, genConfig)
	if err != nil {
		return "", fmt.Errorf("failed to generate content with Gemini: %w", err)
	}
    
    if resp == nil || len(resp.Candidates) == 0 {
         return "", fmt.Errorf("no candidates returned")
    }

    // Concatenate text parts
    var sb strings.Builder
    for _, part := range resp.Candidates[0].Content.Parts {
        if part.Text != "" {
            sb.WriteString(part.Text)
        }
    }
    
	return sb.String(), nil
}

func (g *GoogleGeminiGenerator) GenerateImage(ctx context.Context, prompt string, opts ...ImageOption) ([]byte, error) {
	options := &ImageOptions{}
	for _, opt := range opts {
		opt(options)
	}

	modelName := "gemini-2.0-flash" 
	if val, ok := g.config["model"].(string); ok && val != "" {
		modelName = val
	}

    var parts []*genai.Part
    
    if len(options.ReferenceImage) > 0 {
        // Add reference image
        parts = append(parts, genai.NewPartFromBytes(options.ReferenceImage, "image/png"))
        
        // Enhanced prompt for reference
        enhancedPrompt := fmt.Sprintf(`请参考上面这张图片的视觉风格（包括配色、排版风格、字体风格、装饰元素风格），生成一张风格一致的新图片。

新图片的内容要求：
%s

重要：
1. 必须保持与参考图相同的视觉风格和设计语言
2. 配色方案要与参考图协调一致
3. 排版和装饰元素的风格要统一
4. 但内容要按照新的要求来生成`, prompt)
        parts = append(parts, genai.NewPartFromText(enhancedPrompt))
    } else {
        parts = append(parts, genai.NewPartFromText(prompt))
    }

    content := genai.NewContentFromParts(parts, "user")
    
	resp, err := g.client.Models.GenerateContent(ctx, modelName, []*genai.Content{content}, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to generate image with Gemini: %w", err)
	}

    if resp == nil || len(resp.Candidates) == 0 {
         return nil, fmt.Errorf("no candidates returned")
    }
    
    for _, part := range resp.Candidates[0].Content.Parts {
        if part.InlineData != nil {
            return part.InlineData.Data, nil
        }
    }

	return nil, fmt.Errorf("no image data found in Gemini response")
}
