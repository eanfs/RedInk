package service

import (
	"context"
	"redink-api/config"
)

var (
	Outline *OutlineService
	Image   *ImageService
)

func InitServices(ctx context.Context, cfg *config.AppConfig) error {
	Outline = NewOutlineService(cfg)
	Image = NewImageService(cfg)
	return nil
}
