# Personal Knowledge Base
<p align="center">
  <strong>一个会自我构建的个人知识库。</strong>

  上传文档、构建知识图谱、智能问答 —— 全部由大语言模型驱动。
</p>
<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#这是什么">这是什么？</a> •
  <a href="#系统架构">系统架构</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#致谢">致谢</a> •
  <a href="#许可证">许可证</a>
</p>
<p align="center">
  <a href="README.md">English</a> | 中文
</p>
---
<p align="center">
  <img src="docs/project-architecture.svg" width="100%" alt="系统架构图">
</p>

## 功能特性

- **多格式文档上传** — 支持 PDF、Word、Excel、图片（OCR）和文本文件的自动解析、分类与增量处理
- **两步思维链知识抽取** — LLM 先分析后生成，产出结构化知识词条并追溯来源
- **智能问答（Graph-RAG）** — 基于文档回答问题，支持引用追踪、置信度评分和跨文档验证
- **四信号知识图谱** — 直接关联、来源重叠、Adamic-Adar、关键词重叠四维相关度模型
- **Louvain 社区检测** — 自动发现知识聚类，支持内聚度评分
- **深度研究** — 多步推理、多查询网络搜索，自动合成研究报告
- **双空间架构** — 分析工作台（前台）与管理工作台（后台）分离，共享统一数据
- **项目隔离** — 多项目支持，独立知识库，跨项目切换
- **本地优先** — 所有数据存储在本地 SQLite，无外部云依赖
- **向量语义搜索** — 基于 FAISS 的嵌入检索，支持任意 OpenAI 兼容端点

## 这是什么？

Personal Knowledge Base 是一个跨平台桌面应用，能将你的文档自动转化为有组织、相互关联的知识库。不同于传统 RAG（每次都从零检索回答），本系统**增量构建和维护结构化知识词条**。知识只编译一次并持续更新，而非每次查询都重新推导。

本项目基于 [Karpathy 的 LLM Wiki 模式](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f)，并受 [LLM Wiki](https://github.com/nashsu/llm_wiki) 启发。我们将核心理念实现为一个完整的全栈 Web 应用，并在企业级文档管理与分析方面做了大量增强。

### 与 LLM Wiki 的关键差异

| 维度 | LLM Wiki | Personal Knowledge Base |
|------|----------|------------------------|
| 架构 | 桌面应用（Electron） | 全栈 Web 应用（Spring Boot + Vue 3） |
| 数据库 | 文件型 Wiki | SQLite + MyBatis-Plus ORM |
| 前端 | React | Vue 3 + Element Plus |
| 文档类型 | Markdown、网页 | PDF、Word、Excel、图片、文本 |
| 知识格式 | YAML 前置元数据的 Wiki 页面 | 结构化分类知识词条 |
| 项目模型 | 单一 Wiki | 多项目隔离 |
| 部署方式 | 本地桌面 | Web + Tauri 桌面端 |

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                  前端（Vue 3 + Element Plus）             │
│  ┌──────────────┐     ┌──────────────────────────────┐  │
│  │   前台       │     │       后台                   │  │
│  │  （分析）    │     │    （管理）                  │  │
│  │  首页, 问答, │     │  仪表盘, 来源, 报告          │  │
│  │  深度研究    │     │  图表, 图谱, 配置...         │  │
│  └──────┬───────┘     └──────────┬───────────────────┘  │
└─────────┼────────────────────────┼──────────────────────┘
          │    REST API（HTTP）    │
┌─────────┼────────────────────────┼──────────────────────┐
│         ▼                        ▼                      │
│              后端（Spring Boot 4.1）                    │
│  ┌─────────────┐  ┌──────────┐  ┌───────────┐  ┌────┐ │
│  │  LLM 服务   │  │  向量    │  │  文档上传  │  │图谱│ │
│  │  (对话/     │  │  搜索    │  │  服务      │  │服务│ │
│  │  嵌入)      │  │  (FAISS) │  │           │  │    │ │
│  └──────┬──────┘  └────┬─────┘  └─────┬─────┘  └──┬─┘ │
│         │              │              │             │   │
│  ┌──────▼──────────────▼──────────────▼─────────────▼┐  │
│  │           SQLite 数据库（WAL 模式）               │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
          │
          ▼
   外部 LLM API
   （DeepSeek、SiliconFlow、OpenAI 等）
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3.5 + Element Plus 2.14 + TypeScript + Vite |
| 后端 | Spring Boot 4.1 + MyBatis-Plus 3.5 + Java 25 |
| 数据库 | SQLite（WAL 模式） |
| AI/LLM | OpenAI 兼容 API（DeepSeek、SiliconFlow、OpenAI、Anthropic 等） |
| 向量嵌入 | SiliconFlow Embedding API（BGE-large-zh-v1.5） |
| 向量搜索 | FAISS |
| 可视化 | ECharts（知识图谱） |
| 桌面端 | Tauri 2（可选） |

## 快速开始

### 环境要求

- **Java 21+** 和 Maven 3.8+
- **Node.js 18+** 和 npm 9+
- LLM API 密钥（DeepSeek、OpenAI 或其他 OpenAI 兼容服务商）
- 向量嵌入 API 密钥（SiliconFlow 或其他服务商）

### 1. 克隆仓库

```bash
git clone https://github.com/setsu2420/Personal-KnowledgeBase.git
cd Personal-KnowledgeBase
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入你的 API 密钥：

```bash
# LLM 配置
LLM_API_KEY=sk-your-llm-api-key
LLM_API_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat
LLM_PROVIDER=deepseek

# 向量嵌入配置
EMBEDDING_API_KEY=sk-your-embedding-api-key
EMBEDDING_API_BASE_URL=https://api.siliconflow.cn
EMBEDDING_MODEL=BAAI/bge-large-zh-v1.5
```

### 3. 启动后端

```bash
cd backend-springboot
./mvnw spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 4. 启动前端

```bash
cd frontend-vue
npm install
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

### 5. 访问应用

| 工作空间 | 地址 | 说明 |
|---------|------|------|
| 前台 | http://localhost:5173/portal | 分析工作台：首页、智能问答、深度研究 |
| 后台 | http://localhost:5173/admin | 管理工作台：仪表盘、来源管理、报告、配置 |

## 配置说明

### LLM 配置

支持两种配置方式：

1. **环境变量**（`.env` 文件）— 启动时加载默认配置
2. **Web 界面** — 进入后台 → 系统配置 → 大语言模型，添加/编辑提供商

支持的提供商：DeepSeek、OpenAI、Anthropic、Azure OpenAI、SiliconFlow、通义千问、Moonshot（Kimi）、Google Gemini、Ollama 等。

### 数据库

应用使用 SQLite WAL 模式，数据库文件存储在 `data/app.db`。首次启动时自动初始化数据库表结构。

### 文件上传

上传文件存储在 `uploads/` 目录，最大文件大小 100MB。支持格式：PDF、DOCX、XLSX、PNG、JPG、TXT、MD、CSV。

## 项目结构

```
Personal-KnowledgeBase/
├── frontend-vue/          # Vue 3 前端应用
│   ├── src/
│   │   ├── views/
│   │   │   ├── portal/    # 前台页面（首页、智能问答、深度研究）
│   │   │   └── admin/     # 后台页面（仪表盘、来源、图表、配置等）
│   │   ├── api/           # API 客户端模块
│   │   ├── router/        # Vue Router 路由配置
│   │   └── store/         # Pinia 状态管理
│   └── package.json
├── backend-springboot/    # Spring Boot 后端
│   ├── src/main/java/com/intelligence/platform/
│   │   ├── controller/    # REST API 控制器（20+ 端点）
│   │   ├── service/       # 业务逻辑层
│   │   ├── mapper/        # MyBatis-Plus 数据访问层
│   │   ├── entity/        # 实体类
│   │   └── config/        # 配置类
│   └── src/main/resources/
│       ├── application.properties
│       └── schema-v2.sql  # 数据库建表脚本与种子数据
├── docs/                  # 项目文档
│   ├── architecture.md    # 系统架构详情
│   ├── api-reference.md   # 完整 REST API 文档
│   ├── frontend-pages.md  # 页面描述与导航结构
│   └── project-introduction.md  # 功能需求规格
├── .env.example           # 环境变量模板
└── README.md
```

## 文档

- [系统架构](docs/architecture.md) — 系统架构与技术栈详情
- [API 参考](docs/api-reference.md) — 完整 REST API 文档（100+ 端点）
- [前端页面](docs/frontend-pages.md) — 页面描述与导航结构
- [项目介绍](docs/project-introduction.md) — 详细功能需求规格

## 致谢

核心方法论来源于 **Andrej Karpathy** 的 [LLM Wiki 模式](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f)，描述了使用 LLM 增量构建和维护个人 Wiki 的模式。

特别感谢 Yong Su 的 [LLM Wiki](https://github.com/nashsu/llm_wiki) 项目，其具体的桌面应用实现启发了本项目的诸多功能设计，包括知识图谱相关度模型、社区检测和深度研究工作流。

基于 [Vue 3](https://vuejs.org/)、[Element Plus](https://element-plus.org/)、[Spring Boot](https://spring.io/projects/spring-boot) 和 [ECharts](https://echarts.apache.org/) 构建。

## 许可证

本项目基于 MIT 许可证开源 — 详见 [LICENSE](LICENSE) 文件。
