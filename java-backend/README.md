# RedInk Java Backend

红墨AI图文生成器的Java版本后端，基于Spring Boot和Spring Boot AI框架实现。

## 功能特性

- ✅ **大纲生成**: 支持小红书风格的图文内容大纲生成
- ✅ **图片生成**: 基于AI的图片生成服务，支持多种服务商
- ✅ **历史记录**: 完整的图文内容历史记录管理
- ✅ **配置管理**: 灵活的服务商配置和API管理
- ✅ **流式响应**: 支持SSE(Server-Sent Events)实时进度推送
- ✅ **错误处理**: 完善的异常处理和错误提示

## 技术栈

- **框架**: Spring Boot 3.4.1
- **AI集成**: Spring Boot AI 1.0.0-M2
- **HTTP客户端**: WebClient (Reactor Netty)
- **图片处理**: Thumbnailator
- **数据存储**: 文件系统 (JSON)
- **异步处理**: CompletableFuture + ExecutorService

## API接口

### 核心接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/outline` | 生成大纲 |
| POST | `/api/generate` | 生成图片 (SSE) |
| GET | `/api/images/{taskId}/{filename}` | 获取图片 |
| POST | `/api/retry` | 重试图片生成 |
| POST | `/api/regenerate` | 重新生成图片 |
| GET | `/api/task/{taskId}` | 获取任务状态 |

### 历史记录接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/history` | 创建历史记录 |
| GET | `/api/history` | 获取历史记录列表 |
| GET | `/api/history/{id}` | 获取历史记录详情 |
| PUT | `/api/history/{id}` | 更新历史记录 |
| DELETE | `/api/history/{id}` | 删除历史记录 |
| GET | `/api/history/search` | 搜索历史记录 |
| GET | `/api/history/stats` | 获取统计信息 |
| GET | `/api/history/{id}/download` | 下载历史记录ZIP |

### 配置接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/config` | 获取系统配置 |

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+

### 2. 配置AI服务商

在项目根目录创建 `text_providers.yaml` 和 `image_providers.yaml` 配置文件：

**text_providers.yaml**
```yaml
active_provider: google_gemini
providers:
  google_gemini:
    type: google_gemini
    api_key: YOUR_GOOGLE_API_KEY
    model: gemini-2.0-flash-exp
    temperature: 1.0
    max_output_tokens: 8000
  openai:
    type: openai_compatible
    api_key: YOUR_OPENAI_API_KEY
    base_url: https://api.openai.com/v1
    model: gpt-4
    temperature: 1.0
    max_output_tokens: 8000
```

**image_providers.yaml**
```yaml
active_provider: google_genai
providers:
  google_genai:
    type: google_genai
    api_key: YOUR_GOOGLE_API_KEY
    model: gemini-2.0-flash-exp-image-generation
    temperature: 1.0
    default_aspect_ratio: "3:4"
  openai:
    type: openai
    api_key: YOUR_OPENAI_API_KEY
    model: dall-e-3
    default_size: "1024x1024"
    quality: standard
```

### 3. 构建和运行

```bash
# 进入项目目录
cd java-backend

# 构建项目
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run

# 或者运行jar包
java -jar target/redink-java-backend-1.0.0.jar
```

### 4. 环境变量

可以通过环境变量配置API密钥：

```bash
export OPENAI_API_KEY=your_openai_api_key
export GOOGLE_API_KEY=your_google_api_key
export OPENAI_BASE_URL=your_custom_base_url
```

## 项目结构

```
src/main/java/com/redink/
├── RedInkApplication.java          # 主启动类
├── config/                         # 配置类
│   ├── AiConfig.java              # AI服务配置
│   ├── ConfigManager.java         # 配置管理
│   ├── WebConfig.java             # Web配置
│   └── FileUploadConfig.java      # 文件上传配置
├── controller/                     # REST控制器
│   ├── ApiController.java         # 主要API控制器
│   └── HistoryController.java     # 历史记录控制器
├── service/                        # 服务层
│   ├── ImageGenerationService.java
│   ├── OutlineGenerationService.java
│   ├── HistoryService.java
│   └── impl/                      # 服务实现
├── model/                         # 数据模型
├── util/                          # 工具类
├── exception/                     # 异常处理
└── ...                           # 其他
```

## 配置说明

### 应用配置 (application.yml)

```yaml
server:
  port: 12398
  host: 0.0.0.0

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# AI配置
openai:
  api-key: ${OPENAI_API_KEY:}
  base-url: ${OPENAI_BASE_URL:}

google:
  api-key: ${GOOGLE_API_KEY:}

# 自定义配置
redink:
  cors:
    origins: http://localhost:5173,http://localhost:3000
  image:
    max-concurrent: 15
    auto-retry: 3
```

## 注意事项

1. **图片生成**: 目前图片生成功能需要完善Spring Boot AI的具体实现
2. **多模态输入**: 大纲生成支持图片输入的功能需要进一步测试
3. **性能优化**: 可考虑添加Redis缓存和数据库存储
4. **安全性**: 生产环境需要添加认证和权限控制

## 许可证

本项目基于 MIT 许可证开源。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目！