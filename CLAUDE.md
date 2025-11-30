# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. 项目概述

红墨 - 小红书AI图文生成器，是一个可以通过输入一句话生成完整小红书图文内容的应用。它结合了AI文案生成和图片生成能力，提供了简单易用的Web界面。

## 2. 技术架构

### 后端 (Backend)
- **语言**: Python 3.11+
- **框架**: Flask
- **AI 模型**:
  - Gemini 3 (文案生成)
  - 🍌Nano banana Pro (图片生成)
- **包管理**: uv
- **主要结构**:
  - `backend/app.py` - 主应用入口
  - `backend/config.py` - 配置管理
  - `backend/routes/api.py` - API 路由
  - `backend/services/` - 业务逻辑服务（大纲生成、图片生成、历史记录）
  - `backend/generators/` - AI 生成器工厂和实现
  - `backend/utils/` - 工具函数

### 前端 (Frontend)
- **框架**: Vue 3 + TypeScript
- **构建**: Vite
- **状态管理**: Pinia
- **主要结构**:
  - `frontend/src/App.vue` - 主应用组件
  - `frontend/src/main.ts` - 应用入口
  - `frontend/src/router/index.ts` - 路由配置
  - `frontend/src/stores/` - Pinia 状态管理
  - `frontend/src/components/` - Vue 组件

## 3. 开发命令

### 后端开发
```bash
# 安装依赖
uv sync

# 启动开发服务器
uv run python -m backend.app
# 访问: http://localhost:12398
```

### 前端开发
```bash
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
# 访问: http://localhost:5173

# 构建生产版本
pnpm build

# 预览生产构建
pnpm preview
```

### Docker 部署
```bash
# 单容器部署
docker run -d -p 12398:12398 -v ./output:/app/output histonemax/redink:latest

# 使用 docker-compose
docker-compose up -d
```

## 4. 配置文件

项目使用 YAML 文件进行配置:

- `text_providers.yaml` - 文本生成服务配置
- `image_providers.yaml` - 图片生成服务配置

配置可以通过 Web 界面的设置页面进行可视化管理。

## 5. 主要功能流程

1. **用户输入** - 用户在首页输入主题
2. **大纲生成** - AI 生成 6-9 页的内容大纲
3. **编辑确认** - 用户可以编辑和调整每一页的描述
4. **图片生成** - 并行或逐张生成图片
5. **下载使用** - 一键下载所有生成的图片

## 6. 重要注意事项

- API Key 安全：不要将 API Key 硬编码在代码中，使用配置文件或 Web 界面配置
- 并发限制：GCP 试用账号建议关闭高并发模式
- 镜像构建：Flask 自动检测前端构建产物，支持单容器部署

## Recent Changes
- 002-java-backend-python-compatibility: Added [if applicable, e.g., PostgreSQL, CoreData, files or N/A]
