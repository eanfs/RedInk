<!--
Sync Impact Report
- Version change: 1.0.0 → 1.1.0
- Modified principles: None
- Modified sections: Technology Stack Constraints (backend changed from Python/Flask to Java/Spring Boot)
- Added sections: None
- Removed sections: None
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md - Checked no backend technology-specific references
  - ✅ .specify/templates/spec-template.md - Checked no backend technology-specific references
  - ✅ .specify/templates/tasks-template.md - Checked no backend technology-specific references
  - ✅ All command files - Verified no outdated references
- Follow-up TODOs: None

-->
# 红墨 - 小红书AI图文生成器 Constitution

## Core Principles

### I. 用户体验优先
所有功能设计必须以普通用户的使用体验为核心，避免技术术语和复杂操作。界面必须简洁直观，生成流程必须在30秒内完成主要交互。
**Rationale**: 这是一款面向内容创作者的工具，降低使用门槛是产品成功的关键。

### II. 质量保证
生成的文案必须符合小红书平台风格，图片必须清晰且与主题高度相关。所有AI生成结果必须经过人工验证模板的过滤和优化。
**Rationale**: 高质量的输出是用户信任和复用的基础。

### III. 安全与隐私
API密钥必须加密存储，不得硬编码在代码中。用户输入的内容和生成的结果必须在24小时内自动清理，不保留任何永久存储。
**Rationale**: 保护用户隐私和数据安全是必须遵守的法律和道德标准。

### IV. 可扩展性
支持多种AI模型提供商的切换，包括但不限于Gemini和Nano banana Pro。代码结构必须模块化，便于添加新的生成服务和功能。
**Rationale**: AI技术发展迅速，保持扩展性可以延长项目生命周期。

### V. 性能优化
图片生成必须支持并发处理，单张图片生成时间不得超过10秒。系统必须支持至少10个并发请求。
**Rationale**: 良好的性能可以提升用户体验和系统容量。

## Technology Stack Constraints

- **后端**: Java 17+ with Spring Boot 3.x framework and Spring AI 1.1.0
- **前端**: Vue 3 + TypeScript with Vite build tool
- **AI模型**: 优先支持Gemini 3和Nano banana Pro，必须通过配置文件动态加载
- **部署**: Docker单容器部署和docker-compose集群部署

## Development Workflow

1. **需求分析**: 所有新功能必须有明确的用户需求文档
2. **测试优先**: 核心功能必须有单元测试和集成测试
3. **代码审查**: 所有PR必须经过至少一名其他开发者的审查
4. **持续集成**: 必须通过GitHub Actions自动运行测试和构建
5. **版本发布**: 使用语义化版本控制，每次发布必须有详细的变更日志

## Governance

- 本章程是项目开发的最高准则，所有开发活动必须遵守
- 章程的修改需要经过项目维护者的一致同意，并更新版本号
- 所有代码必须符合章程中规定的原则和约束
- 每月进行一次章程合规性审查

**Version**: 1.1.0 | **Ratified**: 2025-11-30 | **Last Amended**: 2025-11-30