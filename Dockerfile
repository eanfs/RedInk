# ============================================
# 红墨 AI图文生成器 - Docker 镜像
# ============================================

# 阶段1: 构建前端
FROM node:22-slim AS frontend-builder

WORKDIR /app/frontend

# 安装 pnpm
RUN npm install -g pnpm

# 复制前端依赖文件
COPY frontend/package.json frontend/pnpm-lock.yaml ./

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制前端源码
COPY frontend/ ./

# 构建前端
RUN pnpm build

# ============================================
# 阶段2: 构建后端 (Go)
FROM golang:1.24 AS backend-builder

WORKDIR /app/go-backend

# 复制 Go 依赖文件
COPY go-backend/go.mod go-backend/go.sum ./

# 下载依赖
RUN go mod download

# 复制后端源码
COPY go-backend/ ./

# 构建二进制文件
RUN CGO_ENABLED=0 GOOS=linux go build -o redink-server main.go

# ============================================
# 阶段3: 最终镜像
FROM debian:bookworm-slim

WORKDIR /app

# 安装必要依赖 (curl用于健康检查, ca-certificates用于HTTPS请求)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制编译好的后端二进制文件
COPY --from=backend-builder /app/go-backend/redink-server ./

# 复制前端构建产物
COPY --from=frontend-builder /app/frontend/dist ./frontend/dist

# 复制提示词文件 (保持后端预期的目录结构)
COPY backend/prompts/ ./backend/prompts/

# 复制配置模板
COPY docker/text_providers.yaml ./
COPY docker/image_providers.yaml ./

# 创建必要的目录
RUN mkdir -p history output

# 设置环境变量
ENV SERVER_PORT=12398
ENV SERVER_HOST=0.0.0.0

# 暴露端口
EXPOSE 12398

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:12398/api/health || exit 1

# 启动命令
CMD ["./redink-server"]
