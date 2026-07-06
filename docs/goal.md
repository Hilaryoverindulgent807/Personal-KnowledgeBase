# 无人信息领域智能情报分析平台 — 开发目标

***

## 项目定位

面向无人系统（无人机/无人车/无人船/无人潜航器）研究分析人员的专业化智能情报分析平台，基于 Graph-RAG 技术，提供资料管理、智能问答、矛盾检测和报告生成四大核心能力，实现从资料获取到产出报告的完整链路。

***

## 一、前端页面结构

### 1.1 前台首页（index.html）

前台首页采用顶部标签页布局，包含三个主标签和一个知识库区域：

**顶部标签导航（3个）：**

1. **研究报告** — 资料列表管理
   - 展示所有已上传并审核通过的资料列表
   - 支持按文件名/关键词搜索、按格式（PDF/DOCX/XLSX）筛选
   - 每条资料显示：文件名、格式、大小、上传时间、来源
   - 支持查看、编辑、删除操作
   - 底部统计：资料总数、合计大小
   - 提供「+ 上传新资料」按钮跳转到 upload.html
2. **智能问答** — 基于 Graph-RAG 的智能对话
   - 对话界面，支持多轮对话和会话管理
   - 预设高频问题快捷入口
   - 所有回答基于真实资料，结论可溯源
3. **深度研究** — 综合分析与报告生成
   - 生成新报告面板：选择研究主题、时间范围、报告类型
   - 已生成报告列表卡片展示
   - 四类研究场景：技术对比分析、发展趋势研判、风险点识别、综合态势分析

**底部知识库区域（4个卡片）：**

- **研究报告库** → admin-reports.html
- **动态信息库** → admin-info-dynamic.html
- **译丛译著库** → admin-translations.html
- **图表库** → admin-charts.html

每个知识库卡片预留示例内容展示区域，用于后续填充具体信息。

### 1.2 后台管理页面

后台页面保留左侧导航栏结构，包括：

- **信息库（4个子库）：** 研究报告库、动态信息库、译丛译著库、图表库
- **项目库** — 研究项目全生命周期管理
- **组织架构库** — 树形结构组织体系
- **知识图谱** — 可视化网络图展示

***

## 二、功能需求清单

### 2.1 文档解析与词条自动抽取

上传资料后，系统自动执行两步思维链抽取流程（参考 llm_wiki 架构）：

| 需求 | 说明 | 参考来源 |
|------|------|----------|
| 多格式上传 | 支持 Word、PDF、TXT、OFD、图片（OCR）、Excel 等多种格式 | project-introduction.md §4.2.1 |
| 文件夹递归导入 | 保留目录结构，文件夹路径作为分类上下文 | llm_wiki |
| SHA256 增量缓存 | 摄入前检查源文件内容哈希，未变更则自动跳过，节省 LLM token 和时间 | llm_wiki |
| 两步思维链抽取 | Step 1: LLM 分析源文档并提取结构化数据（实体/概念/技术/关系）；Step 2: 基于结构化数据生成知识词条（类似 Wiki 页面） | llm_wiki README |
| 词条类型 | 概念 (concept)、实体 (entity)、技术 (technology)、平台 (platform)、机构 (organization)、方法论 (methodology)、发现 (finding)、资料摘要 (source) | llm_wiki wiki-page-types |
| 词条内容 | 每个词条包含：词条名称、类型、描述、来源资料（可追溯）、关联词条（wikilinks 交叉引用）、抽取时间 | llm_wiki |
| 自动信息提取 | 提取文档元数据、技术领域识别、关键结论和数据、引用关系 | project-introduction.md §4.2.2 |
| MinerU PDF 解析 | 支持云端和本地两种模式，优先本地部署 | llm_wiki |
| 自定义字段 | 上传时可添加来源机构、发布时间、密级、项目编号等扩展字段 | project-introduction.md §4.2.3 |
| 审核流程 | 上传资料解析后以"待审核"状态进入后台，管理人员审核发布后前台可见 | project-introduction.md §4.2.4 |
| 词条展示 | 前台"知识词条"子标签页展示所有自动抽取的词条，支持按类型筛选和搜索 | 当前前端实现 |

### 2.2 智能问答

| 需求           | 说明                             | 参考来源                           |
| ------------ | ------------------------------ | ------------------------------ |
| Graph-RAG 检索 | 基于知识图谱的语义检索，支持跨词汇匹配            | project-introduction.md §4.3.1 |
| 交叉验证         | 多份资料涉及同一主题时自动比对各方说法，找出一致结论和分歧点 | project-introduction.md §4.3.1 |
| 引用溯源         | 回答中明确标注引用的资料和具体章节，可点击追溯        | project-introduction.md §4.3.1 |
| 置信度评分        | 每条回答附带置信度，评分越高说明依据越充分          | project-introduction.md §4.3.1 |
| 多轮对话         | 支持多轮连续对话，每个新话题可建独立对话           | project-introduction.md §4.3.3 |
| 会话持久化        | 会话记录持久化保存，关闭页面不丢失              | project-introduction.md §4.3.3 |
| 快捷提问         | 页面预设高频问题入口                     | project-introduction.md §4.3.4 |

### 2.3 深度研究 / 报告生成

| 需求     | 说明                                  | 参考来源                           |
| ------ | ----------------------------------- | ------------------------------ |
| 技术对比分析 | 对不同技术路线做横向比较，以对比表格或分析段落呈现           | project-introduction.md §4.4.1 |
| 发展趋势研判 | 梳理技术发展脉络、关键拐点和未来走向                  | project-introduction.md §4.4.1 |
| 风险点识别  | 自动检测多份资料之间的结论矛盾和数据不一致，标注严重程度        | project-introduction.md §4.4.1 |
| 综合态势分析 | 全局态势扫描，综合技术发展、装备布局、国际竞争态势           | project-introduction.md §4.4.1 |
| 报告生成流程 | 检索相关资料 → 交叉比对 → 生成结构化初稿 → 人工审核 → 发布 | project-introduction.md §4.4.2 |
| 引用溯源   | 每份报告逐一标注引用资料来源，支持点击跳转原始资料核实         | project-introduction.md §4.4.3 |

### 2.4 知识图谱

| 需求           | 说明                                | 参考来源                                |
| ------------ | --------------------------------- | ----------------------------------- |
| 自动 Embedding | 开启向量搜索时，新页面摄入后自动生成 embedding      | llm\_wiki                           |
| 向量语义搜索       | 可选的 embedding 检索，基于 LanceDB       | llm\_wiki                           |
| 知识图谱可视化      | 可视化网络图展示资料之间的关联关系                 | project-introduction.md §5.8        |
| Louvain 社区检测 | 自动发现知识聚类，内聚度评分                    | llm\_wiki                           |
| 四信号关联度模型     | 直接引用、来源重叠、Adamic-Adar、类型亲和四维关联度模型 | project-introduction.md / llm\_wiki |
| 图谱洞察         | 自动识别孤立节点、稀疏连接区域、核心枢纽节点            | project-introduction.md §5.8        |
| 全局视图         | 支持全屏查看、缩放和拖拽                      | project-introduction.md §5.8        |

### 2.5 矛盾检测

| 需求     | 说明                              | 参考来源                         |
| ------ | ------------------------------- | ---------------------------- |
| 自动矛盾检测 | 资料入库后自动执行交叉比对，识别多份资料之间的矛盾和数据不一致 | project-introduction.md §5.6 |
| 矛盾分类   | 冲突来源、差异分析、严重程度分级（严重/高度/中度）、处置建议 | project-introduction.md §5.6 |
| 前置检测   | 矛盾发现从传统的报告撰写阶段前移至资料入库阶段         | project-introduction.md §5.6 |

### 2.6 后台管理

| 需求     | 说明                           | 参考来源                         |
| ------ | ---------------------------- | ---------------------------- |
| 信息库管理  | 4 个子库的增删改查、搜索筛选、分页浏览         | project-introduction.md §5.1 |
| 项目库    | 项目全生命周期管理，与组织架构库双向关联         | project-introduction.md §5.2 |
| 组织架构库  | 树形结构展示，支持新增/编辑/删除，关联项目       | project-introduction.md §5.3 |
| 智能问答管理 | 查看所有问答历史，抽查回答质量，识别需求热点       | project-introduction.md §5.4 |
| 综合研究管理 | 统一管理四类智能分析报告，审核质量并追踪进度       | project-introduction.md §5.5 |
| 结论冲突管理 | 逐条审查矛盾记录，决定采信方案或安排补充研究       | project-introduction.md §5.6 |
| 方向建议   | 基于资料库数据分析，自动分析覆盖度、空白领域、待解决矛盾 | project-introduction.md §5.7 |
| 系统配置   | 知识图谱权重系数调整、平台基本信息修改          | project-introduction.md §5.9 |

### 2.7 可视化与交互

| 需求     | 说明                            | 参考来源                         |
| ------ | ----------------------------- | ---------------------------- |
| 队列可视化  | 活动面板显示进度条、排队/处理中/失败任务，支持取消和重试 | llm\_wiki                    |
| 文件状态追踪 | 可以看到上传文件的处理状态                 | llm\_wiki                    |
| 双空间架构  | 前台分析空间与后台管理空间，共享统一数据底层        | project-introduction.md §3.2 |
| 权限隔离   | 前台只展示审核通过的资料，后台管理数据发布权限       | project-introduction.md §3.3 |

***

## 三、技术架构

### 3.1 总体架构

分层架构，自下而上：

- **数据层：** 关系型数据库（SQLite WAL 模式）、向量数据库（LanceDB）、文件存储系统
- **服务层：** 文档解析服务、智能问答服务、矛盾检测服务、报告生成服务、知识图谱服务
- **应用层：** 前台分析工作空间 + 后台管理工作空间 Web 界面

### 3.2 核心技术

- **Graph-RAG：** 图增强检索增强生成，传统 RAG + 知识图谱
- **四信号关联度模型：** 直接引用、来源重叠、Adamic-Adar、类型亲和
- **Louvain 社区检测：** 自动发现知识聚类
- **SHA256 增量缓存：** 摄入前哈希校验，避免重复处理
- **MinerU：** PDF 解析引擎（优先本地部署）

### 3.3 技术栈

| 层级     | 技术选型                            |
| ------ | ------------------------------- |
| 前端     | Vue 3 + Element Plus + TypeScript |
| 后端     | Spring Boot 4.1 + MyBatis-Plus  |
| 数据库    | SQLite (WAL 模式) + FAISS（向量检索）  |
| PDF 解析 | MinerU（本地优先）                    |
| 知识图谱   | NetworkX + D3.js（前端可视化）        |
| 社区检测   | Louvain 算法                      |
| 嵌入模型   | 外部大模型 Embedding（如 bge-large-zh，通过 SiliconFlow API） |

***

## 四、开发阶段规划

### 阶段一：基础框架搭建

1. 前后端项目结构初始化
2. 数据库表结构设计（研究报告、动态信息、译丛译著、图表、项目、组织、问答记录、分析报告、冲突记录等）
3. 前端页面骨架搭建（前台 3 标签 + 知识库，后台导航）
4. API 接口定义

### 阶段二：文档摄入与词条抽取

1. 多格式文件上传功能
2. MinerU PDF 解析集成
3. SHA256 增量缓存
4. **两步思维链词条抽取**（参考 llm_wiki）：Step 1 LLM 分析源文档提取结构化数据 → Step 2 生成知识词条
5. 词条类型体系（concept/entity/technology/platform/organization/methodology/finding/source）
6. 词条关联（来源资料追溯 + wikilinks 交叉引用）
7. 前台"知识词条"子标签页展示
8. 文件夹递归导入
9. 审核流程

### 阶段三：智能问答

1. Embedding 自动生成
2. LanceDB 向量检索
3. Graph-RAG 检索机制
4. 交叉验证与置信度评分
5. 多轮对话与会话持久化
6. 引用溯源

### 阶段四：知识图谱

1. 四信号关联度模型实现
2. 知识图谱构建（节点/关系抽取）
3. Louvain 社区检测
4. 图谱可视化（D3.js 或 PyVis）
5. 图谱洞察（孤立节点、稀疏区域、核心枢纽）

### 阶段五：深度研究与矛盾检测

1. 四类研究报告生成
2. 自动矛盾检测
3. 矛盾分级与处置建议
4. 报告审核流程

### 阶段六：后台管理完善

1. 信息库 4 子库完整功能
2. 项目库与组织架构库
3. 问答管理、研究管理、冲突管理
4. 方向建议
5. 系统配置

### 阶段七：测试与优化

1. 端到端测试
2. 性能优化
3. 用户体验优化
4. 文档完善

***

## 五、当前页面状态

### 已完成的前端页面

| 页面 | 状态 |
| --- | --- |
| `frontend-vue/src/views/portal/Home.vue` | ✅ 前台首页（功能卡片入口 + 知识库） |
| `frontend-vue/src/views/portal/ResearchReport.vue` | ✅ 研究报告列表 |
| `frontend-vue/src/views/portal/SmartQA.vue` | ✅ 智能问答 |
| `frontend-vue/src/views/portal/DeepResearch.vue` | ✅ 深度研究 |
| `frontend-vue/src/views/portal/Upload.vue` | ✅ 资料上传 |
| `frontend-vue/src/views/portal/KG.vue` | ✅ 知识图谱 |
| `frontend-vue/src/views/admin/Dashboard.vue` | ✅ 后台仪表盘 |
| `frontend-vue/src/views/admin/InfoDynamic.vue` | ✅ 动态信息管理 |
| `frontend-vue/src/views/admin/Reports.vue` | ✅ 研究报告管理 |
| `frontend-vue/src/views/admin/Translations.vue` | ✅ 译丛译著管理 |
| `frontend-vue/src/views/admin/Charts.vue` | ✅ 图表管理 |
| `frontend-vue/src/views/admin/Projects.vue` | ✅ 项目库管理 |
| `frontend-vue/src/views/admin/Settings.vue` | ✅ 系统配置 |
| `frontend-vue/src/views/admin/KG.vue` | ✅ 知识图谱管理 |
| `frontend-vue/src/views/admin/Sources.vue` | ✅ 资料源管理 |

### 后端 API 对接状态

**已实现：**
- ~~文件上传 API~~ ✅ （`UploadController`，支持多格式上传、SHA256 校验、异步解析队列）
- ~~知识词条 CRUD API~~ ✅ （`KnowledgeEntryController`，词条增删改查与类型筛选）
- ~~问答 API~~ ✅ （`QAController` + `QAChatController`，支持多轮对话与会话管理）
- ~~知识图谱 API~~ ✅ （`KGController`，节点/关系查询与社区检测）
- ~~矛盾检测 API~~ ✅ （`RiskController`，冲突告警查询与管理）
- ~~深度研究 API~~ ✅ （`DeepResearchController`，研究任务创建与结果查询）
- ~~分析报告 API~~ ✅ （`AnalysisController` + `ReportController`）

**待实现 / 待完善：**
- **两步思维链词条抽取 API**（Step 1: LLM 分析提取结构化数据 → Step 2: 生成知识词条）— 待集成
- 审核流程 API — 待实现