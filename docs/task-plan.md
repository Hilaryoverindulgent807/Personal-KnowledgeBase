# V3 重构计划：功能对齐 + 工业级标准

## 状态：全部完成 ✅

## 已完成

### 1. 删除后台页面 [complete]
- 删除：智能问答、分析报告、结论冲突、方向建议
- 更新 AdminLayout 侧边栏
- 删除对应 .vue 文件和路由

### 2. 完善后台系统配置 [complete]
- 参考 llm_wiki 的 settings-view.tsx 分类体系
- 6 个配置分类：LLM / Embedding / 网络搜索 / 图谱构建 / 通用设置 / 关于
- LLM 支持 6 种 Provider + 模型建议 + 连接测试
- 搜索引擎支持 6 种 Provider（DuckDuckGo/Tavily/Google/SerpApi/Brave/SearXNG）
- 图谱构建配置（抽取策略/关系类型/社区算法/重建按钮）

### 3. 知识图谱可视化 [complete]
- 安装 echarts + vue-echarts
- 前台 KG：ECharts force-directed 力导向图
- 后台 KG：图谱可视化 + 数据表格双视图
- 支持按类型/按社区着色
- 节点大小根据连接数动态计算
- 点击节点显示详情

### 4. 知识图谱底层构建 [complete]
- 新增 POST /api/kg/build 端点
- 从知识词条（knowledge_entries）构建图谱
- 基于关键词重叠/同来源/同分类确定关系类型
- BFS 连通分量社区检测
- Settings 页面"重建图谱"按钮已对接

### 5. 工业级标准 [complete]
- TypeScript 严格类型检查通过
- Vite build 成功（997ms）
- Spring Boot compile 成功
- 全局错误处理（GlobalExceptionHandler）
- 统一返回格式（Result/PageResult）
- API 层完整的错误捕获和用户提示

### 6. 前台 HTML 原型图 [complete]
- origin/portal-research-report.html — 研究报告Tab
- origin/portal-smart-qa.html — 智能问答Tab
- origin/portal-deep-research.html — 深度研究Tab

## 构建验证
- Vue TypeScript: ✅ `vue-tsc --noEmit` 通过
- Vue Build: ✅ `vite build` 997ms
- Spring Boot: ✅ `mvnw compile` 成功

## 前后台分工
- **前台**（分析工作空间）：研究报告（词条浏览/上传/4库）、智能问答（Graph-RAG）、深度研究（LLM+网络搜索）
- **后台**（管理工作空间）：仪表盘、信息库（4子库）、项目库、组织架构、知识图谱管理、系统配置（LLM/Embedding/搜索/图谱/通用）