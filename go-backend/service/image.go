package service

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"sync"

	"redink-api/config"
	"redink-api/generators"
)

type ImageService struct {
	cfg *config.AppConfig
	// In-memory task state - simple map for now
	tasks sync.Map 
}

func NewImageService(cfg *config.AppConfig) *ImageService {
	return &ImageService{
		cfg: cfg,
	}
}

func (s *ImageService) GenerateImage(ctx context.Context, prompt string) ([]byte, error) {
	gen, err := s.getImageGenerator()
	if err != nil {
		return nil, err
	}
    
    // Load prompt template if needed, but here we just pass prompt directly for simple test
	return gen.GenerateImage(ctx, prompt)
}

func (s *ImageService) getImageGenerator() (generators.ImageGenerator, error) {
	active := s.cfg.Providers.Image.ActiveProvider
	providers := s.cfg.Providers.Image.Providers
	
	if active == "" {
		return nil, fmt.Errorf("no active image provider")
	}
	
	conf, ok := providers[active].(map[string]interface{})
	if !ok {
		return nil, fmt.Errorf("config for provider %s is invalid", active)
	}
	
    if _, ok := conf["type"]; !ok {
        // Infer or default
        conf["type"] = "openai_compatible" 
    }

	return generators.NewImageGenerator(conf)
}

func (s *ImageService) LoadPromptTemplate(short bool) (string, error) {
    filename := "image_prompt.txt"
    if short {
        filename = "image_prompt_short.txt"
    }
    
    paths := []string{
		"backend/prompts/" + filename,
		"../backend/prompts/" + filename,
		"./prompts/" + filename,
	}
	
	for _, p := range paths {
        absPath, _ := filepath.Abs(p)
		if content, err := os.ReadFile(absPath); err == nil {
			return string(content), nil
		}
        if content, err := os.ReadFile(p); err == nil {
			return string(content), nil
		}
	}
	return "", fmt.Errorf("prompt file %s not found", filename)
}
