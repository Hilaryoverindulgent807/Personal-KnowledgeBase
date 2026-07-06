# 智能情报分析平台 - 启动说明

## 技术栈

- **后端**：Spring Boot 4.1 + MyBatis-Plus + SQLite
- **前端**：Vue 3 + TypeScript + Element Plus + Vite

## 快速启动

### 1. 启动后端

```bash
cd backend-springboot
./mvnw spring-boot:run
```

后端运行在 http://localhost:8080

### 2. 启动前端（开发模式）

```bash
cd frontend-vue
npm install  # 首次需要
npm run dev
```

前端运行在 http://localhost:5173，API 请求自动代理到后端。

## 生产构建

```bash
cd frontend-vue
npm run build
```

构建产物在 `frontend-vue/dist/` 目录。

## 页面地址

### 前台（Portal）

| 页面 | 路径 |
|------|------|
| 首页 | http://localhost:5173/portal |
| 资料上传 | http://localhost:5173/portal/upload |
| 分析报告 | http://localhost:5173/portal/analysis |
| 智能问答 | http://localhost:5173/portal/qa |
| 知识图谱 | http://localhost:5173/portal/kg |

### 后台管理（Admin）

| 页面 | 路径 |
|------|------|
| 仪表盘 | http://localhost:5173/admin |
| 动态信息 | http://localhost:5173/admin/info-dynamic |
| 研究报告 | http://localhost:5173/admin/reports |
| 译丛译著 | http://localhost:5173/admin/translations |
| 图表 | http://localhost:5173/admin/charts |
| 项目库 | http://localhost:5173/admin/projects |
| 组织架构 | http://localhost:5173/admin/org |
| 智能问答 | http://localhost:5173/admin/qa |
| 分析报告 | http://localhost:5173/admin/analysis |
| 结论冲突 | http://localhost:5173/admin/risk |
| 方向建议 | http://localhost:5173/admin/decision |
| 系统配置 | http://localhost:5173/admin/settings |
| 知识图谱 | http://localhost:5173/admin/kg |

## API 接口

后端 API 前缀：`http://localhost:8080/api`

| 模块 | 端点 |
|------|------|
| 仪表盘 | `/api/dashboard/stats`, `/api/dashboard/latest-documents`, `/api/dashboard/active-projects` |
| 资料管理 | `/api/documents` |
| 报告管理 | `/api/reports` |
| 智能问答 | `/api/qa`, `/api/qa/sessions`, `/api/qa/session/{id}` |
| 分析报告 | `/api/analysis`, `/api/analysis/stats` |
| 结论冲突 | `/api/risks`, `/api/risks/stats` |
| 方向建议 | `/api/decisions` |
| 知识图谱 | `/api/kg/nodes`, `/api/kg/edges`, `/api/kg/graph`, `/api/kg/insights` |
| 系统配置 | `/api/settings` |
| 文件上传 | `/api/upload` |
| 统一搜索 | `/api/search`, `/api/search/stats/full` |
| 组织架构 | `/api/organizations`, `/api/organizations/tree` |
| 项目库 | `/api/projects` |

## 项目结构

```
.
├── backend-springboot/     # Spring Boot 后端
│   ├── src/main/java/com/intelligence/platform/
│   │   ├── controller/     # 13 个 REST Controller
│   │   ├── entity/         # 10 个实体类
│   │   ├── mapper/         # 10 个 MyBatis-Plus Mapper
│   │   ├── config/         # 配置类
│   │   └── common/         # 通用类（Result, PageResult, GlobalExceptionHandler）
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend-vue/           # Vue 3 前端
│   ├── src/
│   │   ├── api/            # Axios API 封装
│   │   ├── views/
│   │   │   ├── portal/     # 前台 5 个页面
│   │   │   └── admin/      # 后台 13 个页面
│   │   └── router/         # Vue Router 路由配置
│   ├── vite.config.ts
│   └── package.json
├── data/                   # SQLite 数据库
│   └── app.db
├── config/                 # 配置文件
│   └── mcporter.json
└── docs/                   # 项目文档
```

## 数据库

SQLite 数据库文件：`data/app.db`

Spring Boot 后端通过相对路径 `../data/app.db`（从 `backend-springboot/` 出发）访问数据库。