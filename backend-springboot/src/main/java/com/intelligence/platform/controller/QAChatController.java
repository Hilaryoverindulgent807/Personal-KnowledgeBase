package com.intelligence.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligence.platform.entity.KnowledgeEntry;
import com.intelligence.platform.entity.QARecord;
import com.intelligence.platform.entity.Setting;
import com.intelligence.platform.mapper.KnowledgeEntryMapper;
import com.intelligence.platform.mapper.QARecordMapper;
import com.intelligence.platform.mapper.SettingMapper;
import com.intelligence.platform.service.ImageService;
import com.intelligence.platform.service.LlmService;
import com.intelligence.platform.service.VectorIndex;
import com.intelligence.platform.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能问答控制器（Graph-RAG风格）
 * 参考 llm_wiki 的 chat-panel.tsx
 * 前台"智能问答"Tab核心端点
 *
 * 流程：用户提问 → 知识图谱/词条检索相关上下文 → LLM生成回答 → 标注来源+置信度
 */
@RestController
@RequestMapping("/api/qa-chat")
@CrossOrigin(origins = "*")
public class QAChatController {

    private static final Logger log = LoggerFactory.getLogger(QAChatController.class);

    @Autowired
    private LlmService llmService;
    @Autowired
    private KnowledgeEntryMapper knowledgeEntryMapper;
    @Autowired
    private QARecordMapper qaRecordMapper;
    @Autowired
    private SettingMapper settingMapper;
    @Autowired
    private VectorSearchService vectorSearchService;
    @Autowired
    private ImageService imageService;

    /**
     * 从settings表获取配置值
     */
    private int getSettingInt(String key, int defaultValue) {
        Setting s = settingMapper.selectById(key);
        if (s != null && s.getValue() != null) {
            try { return Integer.parseInt(s.getValue()); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private float getSettingFloat(String key, float defaultValue) {
        Setting s = settingMapper.selectById(key);
        if (s != null && s.getValue() != null) {
            try { return Float.parseFloat(s.getValue()); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    /**
     * 智能问答（Graph-RAG风格 + 多模态结构化返回）
     * 1. 分类检索文本/表格/图片
     * 2. 调用LLM生成回答（文字部分）
     * 3. 结构化返回：text → tables → images，每个携带来源标注
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        String sessionId = body.getOrDefault("sessionId", UUID.randomUUID().toString());

        if (question.isEmpty()) {
            return Map.of("answer", "请输入问题", "confidence", 0.0, "sources", List.of(),
                    "tables", List.of(), "images", List.of());
        }

        try {
            // 1. 分类检索：文本 + 表格 + 图片
            int maxEntries = getSettingInt("qa_max_entries", 10);
            int imageTopK = getSettingInt("qa_image_topK", 5);
            float imageThreshold = getSettingFloat("qa_image_threshold", 0.6f);
            int tableTopK = getSettingInt("qa_table_topK", 3);
            float tableThreshold = getSettingFloat("qa_table_threshold", 0.6f);

            // 文本检索（含表格）
            List<KnowledgeEntry> relevantEntries = searchRelevantEntries(question, maxEntries);
            String context = buildContext(relevantEntries);

            // 图片检索（先用阈值筛选，再用 Top-K 截取）；topK=0 时不返回
            List<Map<String, Object>> imageResults = imageTopK > 0
                    ? searchImages(question, imageTopK, imageThreshold) : List.of();

            // 表格检索（先用阈值筛选，再用 Top-K 截取）；topK=0 时不返回
            List<Map<String, Object>> tableResults = tableTopK > 0
                    ? searchTables(question, tableTopK, tableThreshold) : List.of();

            // 2. 构建系统提示词（指导LLM按 文字→表格→图片 顺序回答）
            // 使用编号引用 [1] [2] 格式，参考 llm_wiki 的引用方式
            String systemPrompt = """
                    你是一个专业的智能情报分析助手。请基于以下知识库内容回答用户的问题。

                    引用规则（严格遵守）：
                    1. 引用信息时，使用方括号编号标注来源，例如 [1]、[2]
                    2. 不要在正文中写出来源名称或文件名，只用编号
                    3. 不要在回答中嵌入"来源：xxx"等文字
                    4. 在回答最末尾添加一个隐藏注释，格式为：<!-- cited: 1, 2, 3 -->，列出你引用的所有编号

                    回答格式要求：
                    1. 首先用文字回答核心问题，引用相关数据
                    2. 如果有相关表格数据，用markdown表格格式呈现
                    3. 如果有相关图表或图片，在文字中说明"参见相关图表"

                    其他规则：
                    1. 优先使用知识库中的信息回答
                    2. 如果知识库中没有相关信息，明确说明
                    3. 回答要准确、结构化、有深度
                    4. 使用中文回答

                    知识库内容：
                    """ + context;

            // 3. 调用LLM
            String answer = llmService.chatWithActive(systemPrompt, question);

            // 3.5 清理回答：移除 <!-- cited: ... --> 隐藏注释和内联来源标记
            answer = cleanAnswer(answer);

            // 4. 计算置信度
            double confidence = relevantEntries.isEmpty() ? 0.3 : Math.min(0.95, 0.5 + relevantEntries.size() * 0.05);

            // 5. 构建来源列表（带编号，供前端引用面板使用）
            List<Map<String, Object>> sources = new ArrayList<>();
            int srcIdx = 0;
            for (KnowledgeEntry e : relevantEntries) {
                if (srcIdx >= 10) break; // 最多10个来源
                Map<String, Object> src = new LinkedHashMap<>();
                src.put("index", srcIdx + 1);
                src.put("entry_id", e.getId());
                src.put("title", e.getTitle());
                src.put("library", e.getLibrary() != null ? e.getLibrary() : "");
                src.put("media_type", e.getMediaType() != null ? e.getMediaType() : "text");
                src.put("source_name", e.getSourceName() != null ? e.getSourceName() : "");
                src.put("source_origin", e.getSourceOrigin() != null ? e.getSourceOrigin() : "");
                src.put("content", e.getContent() != null ? e.getContent().substring(0, Math.min(e.getContent().length(), 150)) + "..." : "");
                sources.add(src);
                srcIdx++;
            }

            // 6. 保存问答记录
            QARecord record = new QARecord();
            record.setQuestion(question);
            record.setAnswer(answer);
            record.setConfidence(confidence);
            record.setSources(sources.toString());
            record.setUserName("分析人员");
            record.setSessionId(sessionId);
            record.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            record.setCategory("");
            qaRecordMapper.insert(record);

            // 7. 结构化返回：text → tables → images
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("answer", answer);
            response.put("confidence", confidence);
            response.put("sources", sources);
            response.put("tables", tableResults);
            response.put("images", imageResults);
            response.put("entry_count", relevantEntries.size());
            response.put("session_id", sessionId);
            return response;

        } catch (Exception e) {
            return Map.of(
                    "answer", "抱歉，系统暂时无法处理您的请求: " + e.getMessage(),
                    "confidence", 0.0,
                    "sources", List.of(),
                    "tables", List.of(),
                    "images", List.of());
        }
    }

    /**
     * 检索相关图片（Top-K + 阈值混合策略）
     */
    private List<Map<String, Object>> searchImages(String question, int topK, float threshold) {
        try {
            List<VectorIndex.SearchResult> results = vectorSearchService.searchWithFilter(
                    question, topK, threshold, "image");
            return results.stream()
                    .map(r -> {
                        Map<String, Object> img = new LinkedHashMap<>();
                        img.put("id", r.id());
                        img.put("score", r.score());
                        img.put("caption", r.metadata().getOrDefault("content", ""));
                        img.put("media_path", r.metadata().getOrDefault("mediaPath", ""));
                        img.put("title", r.metadata().getOrDefault("title", ""));
                        img.put("source_origin", r.metadata().getOrDefault("sourceOrigin", ""));
                        img.put("source_name", r.metadata().getOrDefault("sourceName", ""));
                        return img;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("图片检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 检索相关表格
     */
    private List<Map<String, Object>> searchTables(String question, int topK, float threshold) {
        try {
            List<VectorIndex.SearchResult> results = vectorSearchService.searchWithFilter(
                    question, topK, threshold, "table");
            return results.stream()
                    .map(r -> {
                        Map<String, Object> table = new LinkedHashMap<>();
                        table.put("id", r.id());
                        table.put("score", r.score());
                        table.put("title", r.metadata().getOrDefault("title", ""));
                        // 确保 table_markdown 是标准 Markdown 格式（如果是 HTML 格式则转换）
                        String tableMd = r.metadata().getOrDefault("tableMarkdown", "");
                        if (tableMd != null && tableMd.toLowerCase().contains("<table")) {
                            tableMd = imageService.htmlTableToMarkdown(tableMd);
                        }
                        table.put("table_markdown", tableMd);
                        table.put("source_origin", r.metadata().getOrDefault("sourceOrigin", ""));
                        table.put("source_name", r.metadata().getOrDefault("sourceName", ""));
                        return table;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("表格检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 检索相关知识词条
     * 优先使用向量搜索（语义相似度），回退到关键词搜索
     */
    private List<KnowledgeEntry> searchRelevantEntries(String question, int maxResults) {
        // 优先使用向量搜索（FAISS IndexFlatIP 等价实现）
        try {
            List<VectorIndex.SearchResult> vectorResults = vectorSearchService.search(question, maxResults);
            if (!vectorResults.isEmpty()) {
                log.info("向量搜索命中 {} 条结果", vectorResults.size());
                // 根据向量搜索结果ID获取完整的知识词条
                List<Long> ids = vectorResults.stream()
                        .map(VectorIndex.SearchResult::id)
                        .toList();
                if (!ids.isEmpty()) {
                    return knowledgeEntryMapper.selectBatchIds(ids);
                }
            }
        } catch (Exception e) {
            log.warn("向量搜索失败，回退到关键词搜索: {}", e.getMessage());
        }

        // 回退：关键词搜索
        String[] keywords = Arrays.stream(question.split("[\\s,，。？！、]+"))
                .filter(kw -> kw.length() >= 2)
                .toArray(String[]::new);

        if (keywords.length == 0) {
            return List.of();
        }

        LambdaQueryWrapper<KnowledgeEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeEntry::getStatus, "approved", "pending")
                .and(w -> {
                    for (String kw : keywords) {
                        w.or()
                                .like(KnowledgeEntry::getTitle, kw)
                                .or()
                                .like(KnowledgeEntry::getContent, kw)
                                .or()
                                .like(KnowledgeEntry::getKeywords, kw);
                    }
                })
                .orderByDesc(KnowledgeEntry::getConfidence)
                .last("LIMIT " + maxResults);

        return knowledgeEntryMapper.selectList(wrapper);
    }

    /**
     * 清理回答文本：移除隐藏注释和内联来源标记
     */
    private String cleanAnswer(String answer) {
        if (answer == null) return "";
        // 移除 <!-- cited: ... --> 隐藏注释
        answer = answer.replaceAll("<!--\\s*cited:.*?-->", "");
        // 移除内联来源标记，如 (来源：xxx) 或 （来源：xxx）
        answer = answer.replaceAll("[（(]来源[：:][^）)]*[）)]", "");
        // 移除尾部多余空行
        return answer.trim();
    }

    /**
     * 构建上下文文本
     */
    private String buildContext(List<KnowledgeEntry> entries) {
        if (entries.isEmpty()) {
            return "（知识库中暂无相关内容）";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            KnowledgeEntry e = entries.get(i);
            String entryType = e.getEntryType() != null ? e.getEntryType() : "concept";
            sb.append(String.format("[%d] 【%s】%s\n", i + 1, entryType, e.getTitle()));
            if (e.getContent() != null) {
                sb.append(e.getContent()).append("\n");
            }
            // 如果条目有 tableMarkdown，将其内容也加入上下文
            if (e.getTableMarkdown() != null && !e.getTableMarkdown().isEmpty()) {
                String tableMd = e.getTableMarkdown();
                // 如果是 HTML 格式则转换为 Markdown
                if (tableMd.toLowerCase().contains("<table")) {
                    tableMd = imageService.htmlTableToMarkdown(tableMd);
                }
                sb.append("[table] ").append(e.getTitle()).append("\n");
                sb.append(tableMd).append("\n");
            }
            if (e.getKeywords() != null) {
                sb.append("关键词: ").append(e.getKeywords()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/sessions")
    public List<Map<String, Object>> listSessions() {
        List<QARecord> all = qaRecordMapper.selectList(
                new LambdaQueryWrapper<QARecord>()
                        .ne(QARecord::getSessionId, "")
                        .isNotNull(QARecord::getSessionId)
                        .orderByAsc(QARecord::getCreatedAt));

        return all.stream()
                .filter(r -> r.getSessionId() != null && !r.getSessionId().isEmpty())
                .collect(Collectors.groupingBy(QARecord::getSessionId))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> session = new LinkedHashMap<>();
                    session.put("session_id", e.getKey());
                    session.put("title", e.getValue().get(0).getQuestion());
                    session.put("first_msg", e.getValue().get(0).getCreatedAt());
                    session.put("msg_count", e.getValue().size());
                    return session;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取指定会话的消息
     */
    @GetMapping("/session/{sessionId}")
    public List<QARecord> getSession(@PathVariable String sessionId) {
        return qaRecordMapper.selectList(
                new LambdaQueryWrapper<QARecord>()
                        .eq(QARecord::getSessionId, sessionId)
                        .orderByAsc(QARecord::getCreatedAt));
    }

    /**
     * 删除指定会话的所有消息记录
     */
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        int deleted = qaRecordMapper.delete(
                new LambdaQueryWrapper<QARecord>()
                        .eq(QARecord::getSessionId, sessionId));
        return Map.of("message", "已删除会话 " + sessionId + "（" + deleted + " 条记录）");
    }
}
