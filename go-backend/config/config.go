/*
 * 配置管理模块
 */

package config

import (
	"log"
	"os"

	"gopkg.in/yaml.v3"
)

// AppConfig 应用配置
type AppConfig struct {
	Server struct {
		Host  string `yaml:"host" env:"SERVER_HOST"`
		Port  string `yaml:"port" env:"SERVER_PORT"`
		Debug bool   `yaml:"debug" env:"DEBUG"`
	} `yaml:"server"`
	CORS struct {
		Origins []string `yaml:"origins" env:"CORS_ORIGINS"`
	} `yaml:"cors"`
	Providers struct {
		Image struct {
			ActiveProvider string                 `yaml:"active_provider" env:"IMAGE_ACTIVE_PROVIDER"`
			Providers      map[string]interface{} `yaml:"providers"`
		} `yaml:"image"`
		Text struct {
			ActiveProvider string                 `yaml:"active_provider" env:"TEXT_ACTIVE_PROVIDER"`
			Providers      map[string]interface{} `yaml:"providers"`
		} `yaml:"text"`
	} `yaml:"providers"`
}

// InitConfig 初始化配置
func InitConfig() (*AppConfig, error) {
	// 创建默认配置
	cfg := &AppConfig{
		Server: struct {
			Host  string `yaml:"host" env:"SERVER_HOST"`
			Port  string `yaml:"port" env:"SERVER_PORT"`
			Debug bool   `yaml:"debug" env:"DEBUG"`
		}{
			Host:  "0.0.0.0",
			Port:  "8080",
			Debug: true,
		},
		CORS: struct {
			Origins []string `yaml:"origins" env:"CORS_ORIGINS"`
		}{
			Origins: []string{"*"},
		},
		Providers: struct {
			Image struct {
				ActiveProvider string                 `yaml:"active_provider" env:"IMAGE_ACTIVE_PROVIDER"`
				Providers      map[string]interface{} `yaml:"providers"`
			} `yaml:"image"`
			Text struct {
				ActiveProvider string                 `yaml:"active_provider" env:"TEXT_ACTIVE_PROVIDER"`
				Providers      map[string]interface{} `yaml:"providers"`
			} `yaml:"text"`
		}{
			Image: struct {
				ActiveProvider string                 `yaml:"active_provider" env:"IMAGE_ACTIVE_PROVIDER"`
				Providers      map[string]interface{} `yaml:"providers"`
			}{
				ActiveProvider: "google_genai",
				Providers:      make(map[string]interface{}),
			},
			Text: struct {
				ActiveProvider string                 `yaml:"active_provider" env:"TEXT_ACTIVE_PROVIDER"`
				Providers      map[string]interface{} `yaml:"providers"`
			}{
				ActiveProvider: "google_gemini",
				Providers:      make(map[string]interface{}),
			},
		},
	}

	// 加载image_providers.yaml
	cfg.loadImageProviders()

	// 加载text_providers.yaml
	cfg.loadTextProviders()

	// 从环境变量覆盖配置
	cfg.loadFromEnv()

	return cfg, nil
}

// loadImageProviders 加载图片生成器配置
func (cfg *AppConfig) loadImageProviders() {
	filePath := "../image_providers.yaml"
	if _, err := os.Stat(filePath); err == nil {
		data, err := os.ReadFile(filePath)
		if err != nil {
			log.Printf("Warning: Failed to read image_providers.yaml: %v", err)
			return
		}

		var imageConfig struct {
			ActiveProvider string                 `yaml:"active_provider"`
			Providers      map[string]interface{} `yaml:"providers"`
		}

		if err := yaml.Unmarshal(data, &imageConfig); err != nil {
			log.Printf("Warning: Failed to parse image_providers.yaml: %v", err)
			return
		}

		if imageConfig.ActiveProvider != "" {
			cfg.Providers.Image.ActiveProvider = imageConfig.ActiveProvider
		}
		if imageConfig.Providers != nil {
			cfg.Providers.Image.Providers = imageConfig.Providers
		}
	}
}

// loadTextProviders 加载文本生成器配置
func (cfg *AppConfig) loadTextProviders() {
	filePath := "../text_providers.yaml"
	if _, err := os.Stat(filePath); err == nil {
		data, err := os.ReadFile(filePath)
		if err != nil {
			log.Printf("Warning: Failed to read text_providers.yaml: %v", err)
			return
		}

		var textConfig struct {
			ActiveProvider string                 `yaml:"active_provider"`
			Providers      map[string]interface{} `yaml:"providers"`
		}

		if err := yaml.Unmarshal(data, &textConfig); err != nil {
			log.Printf("Warning: Failed to parse text_providers.yaml: %v", err)
			return
		}

		if textConfig.ActiveProvider != "" {
			cfg.Providers.Text.ActiveProvider = textConfig.ActiveProvider
		}
		if textConfig.Providers != nil {
			cfg.Providers.Text.Providers = textConfig.Providers
		}
	}
}

// loadFromEnv 从环境变量加载配置
func (cfg *AppConfig) loadFromEnv() {
	if host := os.Getenv("SERVER_HOST"); host != "" {
		cfg.Server.Host = host
	}
	if port := os.Getenv("SERVER_PORT"); port != "" {
		cfg.Server.Port = port
	}
	if debug := os.Getenv("DEBUG"); debug != "" {
		cfg.Server.Debug = debug == "true" || debug == "1"
	}
	if corsOrigins := os.Getenv("CORS_ORIGINS"); corsOrigins != "" {
		cfg.CORS.Origins = []string{corsOrigins}
	}
	if imgProvider := os.Getenv("IMAGE_ACTIVE_PROVIDER"); imgProvider != "" {
		cfg.Providers.Image.ActiveProvider = imgProvider
	}
	if textProvider := os.Getenv("TEXT_ACTIVE_PROVIDER"); textProvider != "" {
		cfg.Providers.Text.ActiveProvider = textProvider
	}
}
