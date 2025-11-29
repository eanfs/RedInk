package generators

import "fmt"

func NewTextGenerator(config map[string]interface{}) (TextGenerator, error) {
	typ, _ := config["type"].(string)
	if typ == "openai" || typ == "openai_compatible" {
		return NewOpenAIGenerator(config)
	}
    if typ == "google_gemini" {
        return NewGoogleGeminiGenerator(config)
    }
	return nil, fmt.Errorf("unknown text generator type: %s", typ)
}

func NewImageGenerator(config map[string]interface{}) (ImageGenerator, error) {
	typ, _ := config["type"].(string)
	if typ == "openai" || typ == "openai_compatible" || typ == "image_api" {
		return NewOpenAIGenerator(config)
	}
    if typ == "google_gemini" {
        return NewGoogleGeminiGenerator(config)
    }
	return nil, fmt.Errorf("unknown image generator type: %s", typ)
}