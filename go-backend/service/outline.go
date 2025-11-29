package service

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/cloudwego/hertz/pkg/common/hlog"
	"redink-api/config"
	"redink-api/generators"
)

type OutlineService struct {
	cfg *config.AppConfig
}

func NewOutlineService(cfg *config.AppConfig) *OutlineService {
	return &OutlineService{cfg: cfg}
}

type OutlineResult struct {
	Success   bool                   `json:"success"`
	Outline   string                 `json:"outline"`
	Pages     []map[string]interface{} `json:"pages"`
	HasImages bool                   `json:"has_images"`
	Error     string                 `json:"error,omitempty"`
}

func (s *OutlineService) GenerateOutline(ctx context.Context, topic string, images []string) *OutlineResult {
	hlog.CtxInfof(ctx, "Generating outline for topic: %s", topic)

	prompt, err := s.loadPromptTemplate()
	if err != nil {
		return &OutlineResult{Success: false, Error: "Failed to load prompt template: " + err.Error()}
	}

	fullPrompt := strings.Replace(prompt, "{topic}", topic, 1)
	if len(images) > 0 {
		fullPrompt += fmt.Sprintf("\n\n注意：用户提供了 %d 张参考图片，请在生成大纲时考虑这些图片的内容和风格。这些图片可能是产品图、个人照片或场景图，请根据图片内容来优化大纲，使生成的内容与图片相关联。", len(images))
	}

	gen, err := s.getTextGenerator()
	if err != nil {
		return &OutlineResult{Success: false, Error: err.Error()}
	}

	// Get params from config
	activeProvider := s.cfg.Providers.Text.ActiveProvider
	providers := s.cfg.Providers.Text.Providers
	providerConfig, ok := providers[activeProvider].(map[string]interface{})
	if !ok {
		providerConfig = make(map[string]interface{})
	}

	model, _ := providerConfig["model"].(string)
	if model == "" {
		model = "gpt-3.5-turbo"
	}
	
temperature := 1.0
	if t, ok := providerConfig["temperature"].(float64); ok {
		temperature = t
	}
	
	maxTokens := 8000
	if t, ok := providerConfig["max_output_tokens"].(int); ok {
		maxTokens = t
	}

	text, err := gen.GenerateText(ctx, fullPrompt, 
		generators.WithModel(model),
		generators.WithTemperature(temperature),
		generators.WithMaxTokens(maxTokens),
		generators.WithImages(images),
	)
	
	if err != nil {
		hlog.CtxErrorf(ctx, "Generate text failed: %v", err)
		return &OutlineResult{Success: false, Error: "Text generation failed: " + err.Error()}
	}

	pages := s.parseOutline(text)
	return &OutlineResult{
		Success: true,
		Outline: text,
		Pages: pages,
		HasImages: len(images) > 0,
	}
}

func (s *OutlineService) loadPromptTemplate() (string, error) {
	// Try multiple paths
	paths := []string{
		"backend/prompts/outline_prompt.txt",
		"../backend/prompts/outline_prompt.txt",
		"./prompts/outline_prompt.txt",
	}
	
	for _, p := range paths {
        // Try to find relative to current working directory
        absPath, _ := filepath.Abs(p)
		if content, err := os.ReadFile(absPath); err == nil {
			return string(content), nil
		}
        // Also try just the path provided (relative)
        if content, err := os.ReadFile(p); err == nil {
			return string(content), nil
		}
	}
	return "", fmt.Errorf("outline_prompt.txt not found")
}

func (s *OutlineService) getTextGenerator() (generators.TextGenerator, error) {
	active := s.cfg.Providers.Text.ActiveProvider
	providers := s.cfg.Providers.Text.Providers
	
	if active == "" {
		return nil, fmt.Errorf("no active text provider")
	}
	
	conf, ok := providers[active].(map[string]interface{})
	if !ok {
		return nil, fmt.Errorf("config for provider %s is invalid", active)
	}
	
	// Add active provider info and check type
    if _, ok := conf["type"]; !ok {
        conf["type"] = "openai_compatible" // default
    }
    
	return generators.NewTextGenerator(conf)
}

func (s *OutlineService) parseOutline(text string) []map[string]interface{} {
	var parts []string
	if strings.Contains(text, "<page>") {
		parts = strings.Split(text, "<page>")
	} else {
		parts = strings.Split(text, "---")
	}

	var pages []map[string]interface{}
	re := regexp.MustCompile(`\[(\S+)\]`)

	for i, p := range parts {
		p = strings.TrimSpace(p)
		if p == "" {
			continue
		}

		pageType := "content"
		match := re.FindStringSubmatch(p)
		if len(match) > 1 {
			typeCn := match[1]
			switch typeCn {
			case "封面":
				pageType = "cover"
			case "内容":
				pageType = "content"
			case "总结":
				pageType = "summary"
			}
		}

		pages = append(pages, map[string]interface{}{
			"index": i,
			"type": pageType,
			"content": p,
		})
	}
	return pages
}
