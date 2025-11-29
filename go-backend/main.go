/*
 * RedInk AI图文生成器
 * Go后端实现
 */

package main

import (
	"context"
	"os"
	"path/filepath"
	"time"

	"github.com/cloudwego/hertz/pkg/app"
	"github.com/cloudwego/hertz/pkg/app/server"
	"github.com/cloudwego/hertz/pkg/common/config"
	"github.com/cloudwego/hertz/pkg/common/hlog"
	"github.com/hertz-contrib/cors"
	"github.com/joho/godotenv"

	"redink-api/api"
	appConfig "redink-api/config"
	"redink-api/service"
)

func main() {
	// 设置上下文
	ctx := context.Background()

	// 加载配置文件
	loadEnv()

	// 初始化配置
	cfg, err := appConfig.InitConfig()
	if err != nil {
		hlog.Fatalf("Failed to load config: %v", err)
		os.Exit(1)
	}

	// 初始化服务
	if err := service.InitServices(ctx, cfg); err != nil {
		hlog.Fatalf("Failed to init services: %v", err)
		os.Exit(1)
	}

	// 启动HTTP服务器
	startHTTPServer(cfg)
}

func loadEnv() {
	// 加载.env文件
	err := godotenv.Load()
	if err != nil {
		// 如果没有.env文件，继续执行，使用环境变量
		hlog.Warn("No .env file found, using environment variables")
	}

	// 也尝试加载项目根目录的.env文件
	rootDir, err := filepath.Abs(filepath.Join("./", "../"))
	if err == nil {
		envPath := filepath.Join(rootDir, ".env")
		if _, err := os.Stat(envPath); err == nil {
			godotenv.Load(envPath)
			hlog.Infof("Loaded env file from: %s", envPath)
		}
	}
}

func startHTTPServer(cfg *appConfig.AppConfig) {
	// 创建Hertz服务器
	opts := []config.Option{
		server.WithHostPorts(cfg.Server.Host + ":" + cfg.Server.Port),
		server.WithMaxRequestBodySize(1024 * 1024 * 200), // 200MB
		server.WithReadTimeout(30 * time.Second),
		server.WithWriteTimeout(30 * time.Second),
	}

	s := server.New(opts...)

	// 配置CORS
	corsConfig := cors.DefaultConfig()
	corsConfig.AllowAllOrigins = true
	corsConfig.AllowHeaders = []string{"*"}
	corsConfig.AllowMethods = []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"}
	s.Use(cors.New(corsConfig))

	// 注册路由
	api.RegisterRoutes(s)

	// 静态文件服务 (适配 Docker 环境)
	// 检查 ./frontend/dist 是否存在
	if _, err := os.Stat("./frontend/dist"); err == nil {
		hlog.Info("📦 检测到前端构建产物，启用静态文件托管模式")
		// 托管静态文件
		s.Static("/", "./frontend/dist")
		// 处理 SPA 路由 (404 -> index.html)
		s.NoRoute(func(ctx context.Context, c *app.RequestContext) {
			c.File("./frontend/dist/index.html")
		})
	} else {
		hlog.Info("🔧 前端构建产物未找到，仅启动 API 服务")
	}

	// 启动服务器
	hlog.Infof("🚀 RedInk AI Server is running on http://%s:%s", cfg.Server.Host, cfg.Server.Port)
	s.Spin()
}
