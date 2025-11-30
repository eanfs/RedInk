package generators

import "context"

// GeneratorFactory creates generators based on configuration
type GeneratorFactory struct {
	ImageGenerators map[string]func(config map[string]interface{}) (ImageGenerator, error)
	TextGenerators  map[string]func(config map[string]interface{}) (TextGenerator, error)
}

// ImageGenerator is the interface for image generation
type ImageGenerator interface {
	GenerateImage(ctx context.Context, prompt string, opts ...ImageOption) ([]byte, error)
	ValidateConfig() bool
}

// TextGenerator is the interface for text generation
type TextGenerator interface {
	GenerateText(ctx context.Context, prompt string, opts ...TextOption) (string, error)
}

// ImageOption defines options for image generation
type ImageOption func(*ImageOptions)

type ImageOptions struct {
	AspectRatio     string
	Size            string
	NegativePrompt  string
	ReferenceImage  []byte
	ReferenceImages [][]byte
	Model           string
}

func WithAspectRatio(ratio string) ImageOption {
	return func(o *ImageOptions) {
		o.AspectRatio = ratio
	}
}

func WithImageModel(model string) ImageOption {
	return func(o *ImageOptions) {
		o.Model = model
	}
}

func WithSize(size string) ImageOption {
	return func(o *ImageOptions) {
		o.Size = size
	}
}

func WithReferenceImage(image []byte) ImageOption {
	return func(o *ImageOptions) {
		o.ReferenceImage = image
	}
}

func WithReferenceImages(images [][]byte) ImageOption {
	return func(o *ImageOptions) {
		o.ReferenceImages = images
	}
}

// TextOption defines options for text generation
type TextOption func(*TextOptions)

type TextOptions struct {
	Model           string
	Temperature     float64
	MaxOutputTokens int
	Images          []string // Base64 encoded images or URLs
	SystemPrompt    string
}

func WithModel(model string) TextOption {
	return func(o *TextOptions) {
		o.Model = model
	}
}

func WithTemperature(temp float64) TextOption {
	return func(o *TextOptions) {
		o.Temperature = temp
	}
}

func WithMaxTokens(tokens int) TextOption {
	return func(o *TextOptions) {
		o.MaxOutputTokens = tokens
	}
}

func WithImages(images []string) TextOption {
	return func(o *TextOptions) {
		o.Images = images
	}
}

func WithSystemPrompt(prompt string) TextOption {
	return func(o *TextOptions) {
		o.SystemPrompt = prompt
	}
}
