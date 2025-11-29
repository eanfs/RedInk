/*
 * API 路由定义
 */

package api

import (
	"github.com/cloudwego/hertz/pkg/app/server"
)

// RegisterRoutes 注册所有API路由
func RegisterRoutes(s *server.Hertz) {
	// 创建API前缀
	api := s.Group("/api")

	// 健康检查
	api.GET("/health", HealthCheckHandler)

	// 大纲生成
	api.POST("/outline", GenerateOutlineHandler)

	// 图片生成
	api.POST("/generate", GenerateImagesHandler)

	// 获取图片
	api.GET("/images/:task_id/:filename", GetImageHandler)

	// 重试生成单张图片
	api.POST("/retry", RetrySingleImageHandler)

	// 批量重试失败图片
	api.POST("/retry-failed", RetryFailedImagesHandler)

	// 重新生成图片
	api.POST("/regenerate", RegenerateImageHandler)

	// 获取任务状态
	api.GET("/task/:task_id", GetTaskStateHandler)

	// 历史记录相关API
	history := api.Group("/history")
	{
		history.POST("", CreateHistoryHandler)
		history.GET("", ListHistoryHandler)
		history.GET("/:record_id", GetHistoryHandler)
		history.PUT("/:record_id", UpdateHistoryHandler)
		history.DELETE("/:record_id", DeleteHistoryHandler)
		history.GET("/search", SearchHistoryHandler)
		history.GET("/stats", GetHistoryStatsHandler)
		history.GET("/scan/:task_id", ScanTaskHandler)
		history.POST("/scan-all", ScanAllTasksHandler)
		history.GET("/:record_id/download", DownloadHistoryZipHandler)
	}

	// 配置管理API
	configGroup := api.Group("/config")
	{
		configGroup.GET("", GetConfigHandler)
		configGroup.POST("", UpdateConfigHandler)
		configGroup.POST("/test", TestConnectionHandler)
	}
}