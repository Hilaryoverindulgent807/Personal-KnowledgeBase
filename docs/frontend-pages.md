# 前端页面说明

## 前台页面 (用户侧)

前台采用顶部栏 + Tab 导航布局（PortalLayout），无独立侧栏。顶部栏包含项目选择器和"后台管理"入口。选择项目后通过 Tab 切换页面内容。前端基于 Vue 3 + Element Plus + TypeScript 构建。

**当前活跃页面（PortalLayout 中通过 Tab 加载）：**

| 页面 | 页面名称 | 说明 | 状态 |
|------|----------|------|------|
| PortalLayout.vue | 前台布局容器 | 顶部栏（标题 + 项目选择器 + 后台管理入口）+ Tab 导航 + 内容区 | ✅ |
| Home.vue | 首页（词条百科） | 知识词条卡片展示、搜索过滤、详情弹窗，对接 KnowledgeEntries API | ✅ |
| SmartQA.vue | 智能问答 | Graph-RAG 风格对话式问答，支持会话管理 | ✅ |
| DeepResearch.vue | 深度研究 | 深度研究任务创建、进度跟踪、结果展示 | ✅ |

## 后台页面 (管理侧)

后台采用左侧栏 + 右侧内容区布局（AdminLayout），侧栏使用 el-menu 导航。头部包含项目选择器和"返回前台"按钮。通过 Vue Router 子路由加载各页面。

| 页面 | 路由路径 | 页面名称 | 说明 | 状态 |
|------|----------|----------|------|------|
| AdminLayout.vue | /admin | 后台布局容器 | 侧栏 + 头部 + 内容区布局，侧栏 el-menu 路由导航 | ✅ |
| Dashboard.vue | /admin | 仪表盘 | 统计卡片（资料数/项目数等）、最新资料表格、活跃项目表格 | ✅ |
| InfoDynamic.vue | /admin/info-dynamic | 动态信息 | 动态信息库管理，支持网址添加、文件上传、重新抽取 | ✅ |
| Reports.vue | /admin/reports | 研究报告 | 研究报告库管理，搜索/分页/上传/状态筛选 | ✅ |
| Translations.vue | /admin/translations | 译丛译著 | 译著文件上传（PDF/DOC/TXT/EPUB/MD）、列表管理 | ✅ |
| Charts.vue | /admin/charts | 图表库 | 图片/表格分类上传、OCR 识别、多表格合并、选择模式 | ✅ |
| Projects.vue | /admin/projects | 项目库 | 项目列表（搜索/新建）、点击行进入项目详情页 | ✅ |
| Settings.vue | /admin/settings | 系统配置 | 左侧分类导航 + 右侧配置面板（LLM/OCR/向量嵌入/网络搜索/图谱构建/通用设置/API接入） | ✅ |
| KG.vue | /admin/kg | 知识图谱 | 图谱管理，ECharts 力导向布局、按类型/社区着色、构建图谱 | ✅ |
| Sources.vue | /admin/sources | 来源管理 | 来源文档列表、导入文件夹、刷新来源 | ✅ |

## 前台导航结构

前台不使用侧栏，采用顶部栏 + Tab 导航：

```
┌─────────────────────────────────────────────┐
│  前台 — 分析工作空间     [项目选择器] [后台管理] │  ← 顶部栏
├─────────────────────────────────────────────┤
│  首页  │  智能问答  │  深度研究                │  ← Tab 导航
├─────────────────────────────────────────────┤
│  (Tab 对应内容区)                             │
└─────────────────────────────────────────────┘
```

- 未选择项目时：显示项目选择引导页（选择/新建项目）
- 选择项目后：显示 Tab 导航和对应内容

## 后台侧栏结构

```
┌──────────────────────────┐
│  智能情报管理系统           │  ← 侧栏 Logo
├──────────────────────────┤
│  📊 仪表盘               │  → /admin
│  📋 信息库 (子菜单) ▼     │
│    ├── 动态信息           │  → /admin/info-dynamic
│    ├── 研究报告           │  → /admin/reports
│    ├── 译丛译著           │  → /admin/translations
│    └── 图表              │  → /admin/charts
│  📁 项目库               │  → /admin/projects
│  ⚙️ 系统配置             │  → /admin/settings
│  🔗 知识图谱             │  → /admin/kg
│  📄 来源管理             │  → /admin/sources
└──────────────────────────┘
┌──────────────────────────┐
│ [项目选择器]  [返回前台]   │  ← 头部栏
└──────────────────────────┘
```

## 前后台系统分离

- **前台**（/portal）：面向用户的分析工作空间，采用顶部栏 + Tab 导航，包含首页（词条百科）、智能问答、深度研究三个 Tab
- **后台**（/admin）：面向管理员的数据管理系统，采用左侧栏 + 右侧内容区布局，通过 Vue Router 子路由加载
- **数据共用**：两套系统通过同一个 Spring Boot 后端 + SQLite 数据库存取数据，共享同一个 axios 实例（`/api` 前缀）
- **跳转关系**：前台顶部栏"后台管理"按钮 → `/admin`；后台头部栏"返回前台"按钮 → `/portal`
- **项目隔离**：前后台均支持项目选择器，通过 `X-Project-Id` 请求头实现数据隔离

## 知识图谱展示页面

| 页面 | 位置 | 展示方式 | 说明 |
|------|------|----------|------|
| admin/KG.vue | 后台 /admin/kg | 全屏 | 图谱管理页，ECharts 力导向布局，按类型/社区着色，支持构建图谱 |

**图谱功能**：ECharts 力导向布局、按节点类型/社区两种着色模式、节点/关系/社区计数、重置视图、构建图谱

## 核心组件

`src/components/` 目录下目前仅有 Vite 脚手架默认的 `HelloWorld.vue`，业务组件均内联在各页面 Vue 文件中（未抽取为独立组件）。

| 组件 | 用途 |
|------|------|
| HelloWorld.vue | Vite 脚手架默认组件（未使用） |

各页面内联的关键功能模块：
- **Home.vue**：知识词条卡片列表、搜索过滤、详情弹窗（使用 marked 渲染 Markdown）
- **SmartQA.vue**：对话气泡、置信度显示、历史会话面板
- **admin/Settings.vue**：左侧分类导航 + 右侧动态配置面板
- **admin/Charts.vue**：图片/表格双 Tab、OCR 识别、批量合并

## API 模块

`src/api/index.ts` 统一导出所有 API 函数，基于同一个 axios 实例（`baseURL: /api`），请求拦截器自动附加 `X-Project-Id` 头和时间戳防缓存参数。Tauri 桌面环境下直连 `http://localhost:8080/api`，Web 环境通过 Vite proxy。

| 分组 | 导出函数 | 用途 |
|------|---------|------|
| Projects | getProjects, createProject, deleteProject, getProjectDetail | 项目 CRUD、项目隔离 |
| Dashboard | getDashboardStats, getLatestDocuments, getActiveProjects | 仪表盘统计数据 |
| Documents | getDocuments, getDocument, createDocument, updateDocument, deleteDocument | 资料 CRUD、筛选分页 |
| Reports | getReports, getReport, createReport, deleteReport | 研究报告管理 |
| QA | getQAList, getQAStats, getQASessions, getQASession, deleteQASession, createQA, deleteQA | 问答记录管理 |
| Analysis | getAnalysisList, getAnalysisStats, getAnalysis, createAnalysis, deleteAnalysis | 分析报告管理 |
| Risks | getRiskList, getRiskStats, createRisk, deleteRisk | 风险项管理 |
| Decisions | getDecisionList, createDecision, deleteDecision | 决策项管理 |
| KG | getKGNodes, getKGEdges, getKGGraph, getKGInsights, buildKGGraph | 知识图谱节点/边/图谱/洞察/构建 |
| Settings | getSettings, updateSettings | 系统配置读写 |
| Search Config | getSearchConfig, saveSearchConfig, testSearchConfig | 搜索配置管理 |
| Upload | uploadFile, uploadFromUrl, checkFileHash, getFileTypes, getLibraries, analyzeDocument, getUploadTaskStatus, getAllUploadTasks, cancelUploadTask | 文件上传、任务状态/取消 |
| Search | unifiedSearch, getFullStats | 统一搜索、全文统计 |
| Knowledge Entries | getKnowledgeEntries, getKnowledgeEntry, createKnowledgeEntry, updateKnowledgeEntry, reviewKnowledgeEntry, batchReviewEntries, deleteKnowledgeEntry, updateTableMarkdown, getEntryStatsByLibrary | 知识词条 CRUD、审核、批量审核、表格 Markdown |
| LLM Config | getLlmConfigs, getActiveLlmConfig, getActiveEmbeddingConfig, getLlmConfig, createLlmConfig, updateLlmConfig, toggleLlmConfig, deleteLlmConfig, testLlmConnection | LLM 配置管理、连接测试 |
| Vector | getVectorStats, rebuildVectorIndex, vectorSearch | FAISS 向量索引管理、语义搜索 |
| QA Chat | askQuestion, getQAChatSessions, getQAChatSession, deleteQAChatSession | Graph-RAG 智能问答（5分钟超时） |
| Deep Research | getDeepResearches, getDeepResearch, createDeepResearch, cancelDeepResearch, deleteDeepResearch | 深度研究任务管理 |
| Sources | getSourceTree, importSourceFolder, importSourceFile, deleteSource, refreshSources | 来源文件管理、文件夹导入 |
| Media | getMediaUrl, getPdfCoverUrl, getDocFileUrl, ocrTableImage, mergeTableEntries, ocrExistingEntries, reprocessImages | 媒体文件访问、OCR 识别、表格合并 |

**辅助函数**：setCurrentProjectId, getCurrentProjectId（项目 ID 管理，localStorage 持久化）

## 标签体系

一级标签和二级标签在数据库和下拉菜单中使用首字母缩写大写形式：

| 一级标签 | 缩写 | 二级标签示例 |
|---------|------|------------|
| 战略规划 | ZLGH | DCZL(顶层战略), FZJH(发展计划) |
| 作战理论 | ZZLL | ZZGN(作战概念), ZZGX(作战构想), ZZTL(作战条令) |
| 装备发展 | ZBFZ | KZWRZB(空中无人装备), DMWRZB, SMWRZB, SXWRZB |
| 关键技术 | GJJS | PTZTJS, ZZJS, XTJQJS, TXZWJS, DLNYJS, RWZHJS, QTBZJS |
| 作战力量 | ZZLLI | XTBZMS, ZZDYBC, BLJGTX, RCJSDW |
| 作战运用 | ZZYY | ZHSY(作战实验), JSYX(军事演习), SZYY(实战运用) |
| 典型项目 | DXXM | WRXM(无人项目), ZNXXM(智能项目) |

## 敏感信息

所有人名已模糊化为角色代号（研究员A-F、教授A-F、分析员A、工程师A），机构名已脱敏。

## 数据流

Vue 3 SPA 通过 axios 实例与后端 REST API 通信，支持 Tauri 桌面和 Web 两种部署模式：

```
Vue 3 组件 → api/index.ts 导出函数 → axios('/api/...')
                                      ↓ (请求拦截器: +X-Project-Id +时间戳防缓存)
                              Spring Boot Controller → MyBatis-Plus → SQLite 数据库
```

- **Web 模式**：baseURL 为 `/api`，通过 Vite dev server proxy 转发到后端 `localhost:8080`
- **Tauri 模式**：baseURL 为 `http://localhost:8080/api`，直连后端

## 已废弃/未使用的页面

以下 Vue 文件存在于 `portal/` 目录中，但未被 PortalLayout 或任何其他组件引用（文件保留但无路由/Tab 加载）：

- **portal/Analysis.vue** — 分析报告列表页（表格+搜索+分页），早期独立页面，现已被 admin 端分析功能替代
- **portal/KG.vue** — 知识图谱展示页（ECharts 力导向布局），早期前台图谱页，现仅在后台 admin/KG.vue 使用
- **portal/QA.vue** — 简单问答页（非 Graph-RAG），已被 SmartQA.vue 替代
- **portal/ResearchReport.vue** — 研究报告页（知识库列表+上传），功能已整合到后台 Reports.vue
- **portal/Upload.vue** — 资料上传页（目标库选择+分类联动），功能已分散到各后台信息库的上传对话框中
