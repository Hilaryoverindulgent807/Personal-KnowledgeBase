# Personal Knowledge Base

A modern personal knowledge management platform built with Graph-RAG technology, featuring intelligent document processing, AI-powered Q&A, and knowledge graph visualization.

## Features

- **Multi-format Document Upload** — Support for PDF, Word, Excel, images (OCR), and text files with automatic parsing and classification
- **Intelligent Knowledge Extraction** — Two-step chain-of-thought extraction using LLM to generate structured knowledge entries
- **Smart Q&A (Graph-RAG)** — Ask questions about your documents with citation tracking, confidence scoring, and cross-document verification
- **Deep Research** — Multi-step reasoning and cross-source synthesis for complex research tasks
- **Knowledge Graph Visualization** — Interactive graph view with 4-signal relevance model and community detection
- **Dual-Space Architecture** — Separate portal (analysis) and admin (management) workspaces sharing unified data
- **Local-First Security** — All data stored locally, no external cloud dependencies
- **Project Isolation** — Multi-project support with independent knowledge bases

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 + Element Plus + TypeScript + Vite |
| Backend | Spring Boot 4.1 + MyBatis-Plus |
| Database | SQLite (WAL mode) |
| AI/LLM | OpenAI-compatible API (DeepSeek, SiliconFlow, etc.) |
| Embedding | SiliconFlow Embedding API (BGE-large-zh) |
| Visualization | ECharts (knowledge graph) |

## Project Structure

```
Personal-KnowledgeBase/
├── frontend-vue/          # Vue 3 frontend application
│   ├── src/
│   │   ├── views/
│   │   │   ├── portal/    # Portal pages (Home, SmartQA, DeepResearch, Upload, KG)
│   │   │   └── admin/     # Admin pages (Dashboard, Sources, Reports, Charts, Settings, etc.)
│   │   ├── api/           # API client modules
│   │   ├── router/        # Vue Router configuration
│   │   └── store/         # Pinia state management
│   └── package.json
├── backend-springboot/    # Spring Boot backend
│   ├── src/main/java/com/intelligence/platform/
│   │   ├── controller/    # REST API controllers
│   │   ├── service/       # Business logic
│   │   ├── mapper/        # MyBatis-Plus data access
│   │   ├── model/         # Entity classes
│   │   └── config/        # Configuration
│   └── src/main/resources/
│       ├── application.properties
│       └── schema-v2.sql  # Database schema
├── docs/                  # Project documentation
│   ├── architecture.md
│   ├── api-reference.md
│   ├── frontend-pages.md
│   ├── project-introduction.md
│   └── prototypes/        # UI design prototypes
├── .env.example           # Environment variables template
└── README.md
```

## Quick Start

### Prerequisites

- **Java 21+** and Maven 3.8+
- **Node.js 18+** and npm 9+
- LLM API key (DeepSeek, OpenAI, or other OpenAI-compatible provider)
- Embedding API key (SiliconFlow or other provider)

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/Personal-KnowledgeBase.git
cd Personal-KnowledgeBase
```

### 2. Configure Environment Variables

```bash
cp .env.example .env
```

Edit `.env` and fill in your API keys:

```bash
LLM_API_KEY=sk-your-llm-api-key
LLM_API_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat

EMBEDDING_API_KEY=sk-your-embedding-api-key
EMBEDDING_API_BASE_URL=https://api.siliconflow.cn
EMBEDDING_MODEL=BAAI/bge-large-zh-v1.5
```

### 3. Start the Backend

```bash
cd backend-springboot
./mvnw spring-boot:run
```

Backend will be available at `http://localhost:8080`.

### 4. Start the Frontend

```bash
cd frontend-vue
npm install
npm run dev
```

Frontend will be available at `http://localhost:5173`.

### 5. Access the Application

- **Portal** (Analysis Workspace): http://localhost:5173/portal
- **Admin** (Management Workspace): http://localhost:5173/admin

## Configuration

### LLM Settings

You can configure LLM providers in two ways:

1. **Environment Variables** (`.env` file) — Default configuration loaded at startup
2. **Web UI** — Go to Admin → Settings → LLM Configuration to add/edit providers

Supported providers: DeepSeek, OpenAI, Anthropic, Azure OpenAI, SiliconFlow, Qwen (Tongyi), Moonshot (Kimi), Google Gemini, Ollama, and more.

### Database

The application uses SQLite with WAL mode. Database file is stored at `data/app.db`. The schema is automatically initialized on first startup.

### File Upload

Uploaded files are stored in `uploads/` directory. Maximum file size: 100MB.

## Documentation

- [Architecture Overview](docs/architecture.md) — System architecture and tech stack details
- [API Reference](docs/api-reference.md) — Complete REST API documentation
- [Frontend Pages](docs/frontend-pages.md) — Page descriptions and navigation structure
- [Project Introduction](docs/project-introduction.md) — Detailed feature specifications

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  Frontend (Vue 3)                │
│  ┌──────────────┐     ┌──────────────────────┐  │
│  │   Portal     │     │       Admin          │  │
│  │  (Analysis)  │     │    (Management)      │  │
│  └──────┬───────┘     └──────────┬───────────┘  │
└─────────┼────────────────────────┼──────────────┘
          │    REST API (HTTP)     │
┌─────────┼────────────────────────┼──────────────┐
│         ▼                        ▼               │
│              Backend (Spring Boot)               │
│  ┌─────────────┐  ┌──────────┐  ┌───────────┐  │
│  │  LLM Service│  │  Vector  │  │  Document  │  │
│  │  (Chat/     │  │  Search  │  │  Upload    │  │
│  │  Embedding) │  │  (FAISS) │  │  Service   │  │
│  └──────┬──────┘  └────┬─────┘  └─────┬─────┘  │
│         │              │              │          │
│  ┌──────▼──────────────▼──────────────▼──────┐  │
│  │           SQLite Database                  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
          │
          ▼
   External LLM APIs
   (DeepSeek, SiliconFlow, etc.)
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by [LLM Wiki](https://github.com/nashsu/llm_wiki) project and Andrej Karpathy's LLM Wiki pattern
- Built with [Vue 3](https://vuejs.org/), [Element Plus](https://element-plus.org/), [Spring Boot](https://spring.io/projects/spring-boot)
