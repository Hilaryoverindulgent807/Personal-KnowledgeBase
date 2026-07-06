-- V2 新增表结构
-- 在现有 app.db 上执行

-- 知识词条表（LLM抽取）
CREATE TABLE IF NOT EXISTS knowledge_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    entry_type TEXT DEFAULT 'concept',
    library TEXT DEFAULT 'report',
    document_id INTEGER,
    source_name TEXT,
    content TEXT,
    keywords TEXT,
    category_l1 TEXT,
    category_l2 TEXT,
    status TEXT DEFAULT 'pending',
    confidence REAL DEFAULT 0.8,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    reviewer TEXT,
    embedding TEXT,  -- 向量嵌入（JSON数组，用于向量搜索加速）
    media_type TEXT DEFAULT 'text',  -- 媒体类型：text/table/image
    media_path TEXT,                  -- 媒体文件路径（图片/文件的磁盘路径）
    source_origin TEXT,               -- 来源信息（论文名、blog URL、PPT页码等）
    table_markdown TEXT               -- 表格的Markdown格式存储
);

-- 向量索引（内存中维护，持久化到 uploads/vector-index.json）

-- LLM配置表
CREATE TABLE IF NOT EXISTS llm_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    api_key TEXT,
    model TEXT NOT NULL,
    base_url TEXT,
    enabled INTEGER DEFAULT 1,
    purpose TEXT DEFAULT 'chat',
    max_context_size INTEGER DEFAULT 4096,
    api_mode TEXT,
    azure_api_version TEXT,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    description TEXT
);

-- 深度研究表
CREATE TABLE IF NOT EXISTS deep_researches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    topic TEXT NOT NULL,
    status TEXT DEFAULT 'queued',
    search_queries TEXT,
    source_count INTEGER DEFAULT 0,
    synthesis TEXT,
    progress INTEGER DEFAULT 0,
    error TEXT,
    llm_config TEXT,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    completed_at TEXT
);

-- ============================================================
-- 初始化数据：中国LLM提供商配置模板（用户需填入自己的API Key）
-- ============================================================
INSERT OR IGNORE INTO llm_configs (name, provider, model, base_url, enabled, purpose, description) VALUES
    ('DeepSeek-V3', 'deepseek', 'deepseek-chat', NULL, 0, 'chat', '深度求索V3模型，高性价比'),
    ('DeepSeek-R1', 'deepseek', 'deepseek-reasoner', NULL, 0, 'chat', '深度求索R1推理模型'),
    ('通义千问-Max', 'qwen', 'qwen-max', NULL, 0, 'chat', '阿里云通义千问旗舰模型'),
    ('通义千问-Plus', 'qwen', 'qwen-plus', NULL, 0, 'chat', '阿里云通义千问增强模型'),
    ('Moonshot-v1-128k', 'moonshot', 'moonshot-v1-128k', NULL, 0, 'chat', '月之暗面Kimi 128K上下文'),
    ('Moonshot-v1-32k', 'moonshot', 'moonshot-v1-32k', NULL, 0, 'chat', '月之暗面Kimi 32K上下文'),
    ('Step-2-16K', 'stepfun', 'step-2-16k', NULL, 0, 'chat', '阶跃星辰Step-2模型'),
    ('混元-Pro', 'hunyuan', 'hunyuan-pro', NULL, 0, 'chat', '腾讯混元旗舰模型'),
    ('混元-Standard', 'hunyuan', 'hunyuan-standard', NULL, 0, 'chat', '腾讯混元标准模型'),
    ('豆包-Pro', 'doubao', 'doubao-pro-32k', NULL, 0, 'chat', '字节跳动豆包Pro 32K模型'),
    ('豆包-Lite', 'doubao', 'doubao-lite-32k', NULL, 0, 'chat', '字节跳动豆包Lite 32K模型'),
    ('MiniMax-Text-01', 'minimax', 'MiniMax-Text-01', NULL, 0, 'chat', 'MiniMax旗舰模型');

-- ============================================================
-- 初始化数据：搜索配置（网络搜索开关和默认设置）
-- ============================================================
INSERT OR IGNORE INTO settings (key, value, description) VALUES
    ('search_enabled', 'true', '网络搜索开关（true=启用，false=禁用）'),
    ('search_provider', 'baidu', '搜索引擎提供商（baidu/sogou/google/brave/tavily/serpapi/searxng/duckduckgo）'),
    ('search_baidu_api_key', '', '百度API Key（可选）'),
    ('search_baidu_secret_key', '', '百度Secret Key（可选）'),
    ('search_google_api_key', '', 'Google Custom Search API Key'),
    ('search_google_cx', '', 'Google Custom Search CX'),
    ('search_api_key', '', '其他搜索API Key（Brave/Tavily/SerpApi通用）'),
    ('search_searxng_url', '', 'SearXNG自托管地址'),
    ('research_max_sources', '20', '深度研究最大来源数量'),
    ('research_max_results_per_query', '10', '每个查询最大结果数');

-- ============================================================
-- 初始化数据：文件上传配置
-- ============================================================
INSERT OR IGNORE INTO settings (key, value, description) VALUES
    ('upload_max_file_size', '104857600', '上传文件最大大小（字节），默认100MB'),
    ('upload_allowed_types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,markdown,csv,jpg,jpeg,png,gif,bmp,webp', '允许上传的文件类型');

-- ============================================================
-- V3 多模态支持：为现有数据库添加新字段
-- ============================================================
ALTER TABLE knowledge_entries ADD COLUMN media_type TEXT DEFAULT 'text';
ALTER TABLE knowledge_entries ADD COLUMN media_path TEXT;
ALTER TABLE knowledge_entries ADD COLUMN source_origin TEXT;
ALTER TABLE knowledge_entries ADD COLUMN table_markdown TEXT;

-- 图片检索配置
INSERT OR IGNORE INTO settings (key, value, description) VALUES
    ('qa_image_topK', '5', '问答图片检索最大返回数量'),
    ('qa_image_threshold', '0.6', '问答图片检索最低相似度阈值（余弦相似度）'),
    ('qa_image_enabled', 'true', '问答中是否返回相关图片');

-- documents表添加来源字段（对齐 llm_wiki 来源机制）
ALTER TABLE documents ADD COLUMN source_origin TEXT;
ALTER TABLE documents ADD COLUMN source_path TEXT;
ALTER TABLE documents ADD COLUMN source_identity TEXT;
ALTER TABLE documents ADD COLUMN folder_context TEXT;
