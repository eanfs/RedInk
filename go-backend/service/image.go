package service

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"redink-api/config"
	"redink-api/generators"

	"github.com/cloudwego/hertz/pkg/common/hlog"
)

type ImagePage struct {
	Index   int    `json:"index"`
	Type    string `json:"type"`
	Content string `json:"content"`
}

type GenerationEvent struct {
	Event string      `json:"event"`
	Data  interface{} `json:"data"`
}

type ImageService struct {
	cfg *config.AppConfig
	// In-memory task state - simple map for now
	tasks               sync.Map
	promptTemplate      string
	promptTemplateShort string
}

func NewImageService(cfg *config.AppConfig) *ImageService {
	s := &ImageService{
		cfg: cfg,
	}
	// Best effort loading
	s.promptTemplate, _ = s.LoadPromptTemplate(false)
	s.promptTemplateShort, _ = s.LoadPromptTemplate(true)
	return s
}

type ImageGenerationParams struct {
	Prompt          string
	AspectRatio     string
	Model           string
	ReferenceImages [][]byte
}

func (s *ImageService) GenerateImage(ctx context.Context, params ImageGenerationParams) ([]byte, error) {
	gen, err := s.getImageGenerator()
	if err != nil {
		return nil, err
	}

	opts := []generators.ImageOption{
		generators.WithAspectRatio(params.AspectRatio),
		generators.WithReferenceImages(params.ReferenceImages),
	}
	if params.Model != "" {
		opts = append(opts, generators.WithImageModel(params.Model))
	}

	return gen.GenerateImage(ctx, params.Prompt, opts...)
}

func (s *ImageService) GenerateImages(ctx context.Context, pages []ImagePage, taskID string, fullOutline string, userImages [][]byte, userTopic string) <-chan GenerationEvent {
	ch := make(chan GenerationEvent)
	go func() {
		defer close(ch)

		// Ensure task dir exists
		// Assuming running from root or similar, adjust path as needed
		// Python uses: os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "history"
		// We'll use a relative "history" for now or config based
		historyDir := "history"
		taskDir := filepath.Join(historyDir, taskID)
		os.MkdirAll(taskDir, 0755)

		total := len(pages)
		hlog.Infof("Starting image generation for task %s with %d pages", taskID, total)

		for i, page := range pages {
			hlog.Debugf("Processing page %d/%d", i+1, total)
			// Send progress
			ch <- GenerationEvent{
				Event: "progress",
				Data: map[string]interface{}{
					"index":   page.Index,
					"status":  "generating",
					"current": i + 1,
					"total":   total,
					"phase":   "content",
				},
			}

			// Format prompt
			prompt := fmt.Sprintf("Content: %s\nType: %s", page.Content, page.Type)
			tmpl := s.promptTemplate
			if tmpl != "" {
				// Simple replacement
				r := strings.NewReplacer(
					"{page_content}", page.Content,
					"{page_type}", page.Type,
					"{full_outline}", fullOutline,
					"{user_topic}", userTopic,
				)
				prompt = r.Replace(tmpl)
			}

			params := ImageGenerationParams{
				Prompt:          prompt,
				AspectRatio:     "3:4", // Should come from config
				ReferenceImages: userImages,
			}

			// TODO: Handle cover image specific logic (using it as reference for others)

			imgData, err := s.GenerateImage(ctx, params)
			if err != nil {
				ch <- GenerationEvent{
					Event: "error",
					Data: map[string]interface{}{
						"index":   page.Index,
						"status":  "error",
						"message": err.Error(),
						"phase":   "content",
					},
				}
				continue
			}

			// Save image
			filename := fmt.Sprintf("%d.png", page.Index)
			os.WriteFile(filepath.Join(taskDir, filename), imgData, 0644)

			ch <- GenerationEvent{
				Event: "complete",
				Data: map[string]interface{}{
					"index":     page.Index,
					"status":    "done",
					"image_url": fmt.Sprintf("/api/images/%s/%s", taskID, filename),
					"phase":     "content",
				},
			}
		}

		ch <- GenerationEvent{
			Event: "finish",
			Data: map[string]interface{}{
				"success": true,
				"task_id": taskID,
			},
		}
	}()
	return ch
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
