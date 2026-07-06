package com.intelligence.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.intelligence.platform.common.PageResult;
import com.intelligence.platform.entity.KnowledgeEntry;
import com.intelligence.platform.mapper.KnowledgeEntryMapper;
import com.intelligence.platform.service.ProjectContext;
import com.intelligence.platform.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识词条管理（前台"研究报告"Tab核心数据源）
 */
@RestController
@RequestMapping("/api/knowledge-entries")
@CrossOrigin(origins = "*")
public class KnowledgeEntryController {

    @Autowired
    private KnowledgeEntryMapper knowledgeEntryMapper;

    @Autowired
    private ProjectContext projectContext;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private KGController kgController;

    /**
     * 分页查询词条
     * 前台"研究报告"Tab调用
     */
    @GetMapping
    public PageResult<KnowledgeEntry> list(
            @RequestParam(required = false) String library,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        LambdaQueryWrapper<KnowledgeEntry> wrapper = new LambdaQueryWrapper<>();
        // 项目隔离
        Long pid = projectContext.getCurrentProjectId();
        if (pid != null) wrapper.eq(KnowledgeEntry::getProjectId, pid);
        if (library != null && !library.isEmpty()) wrapper.eq(KnowledgeEntry::getLibrary, library);
        if (entryType != null && !entryType.isEmpty()) wrapper.eq(KnowledgeEntry::getEntryType, entryType);
        if (status != null && !status.isEmpty()) wrapper.eq(KnowledgeEntry::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(KnowledgeEntry::getTitle, keyword)
                    .or().like(KnowledgeEntry::getContent, keyword)
                    .or().like(KnowledgeEntry::getKeywords, keyword));
        }
        wrapper.orderByDesc(KnowledgeEntry::getCreatedAt);

        Page<KnowledgeEntry> pageObj = new Page<>(page, pageSize);
        Page<KnowledgeEntry> result = knowledgeEntryMapper.selectPage(pageObj, wrapper);
        // SQLite 不支持 COUNT(*) 在 selectPage 中，使用 items.size() 作为 fallback
        long total = result.getTotal() > 0 ? result.getTotal() : result.getRecords().size();
        return new PageResult<>(total, page, pageSize, result.getRecords());
    }

    /**
     * 按资料库统计词条数量（带项目隔离）
     */
    @GetMapping("/stats/by-library")
    public Map<String, Long> statsByLibrary() {
        Long pid = projectContext.getCurrentProjectId();

        LambdaQueryWrapper<KnowledgeEntry> reportW = new LambdaQueryWrapper<KnowledgeEntry>().eq(KnowledgeEntry::getLibrary, "report");
        LambdaQueryWrapper<KnowledgeEntry> dynamicW = new LambdaQueryWrapper<KnowledgeEntry>().eq(KnowledgeEntry::getLibrary, "dynamic");
        LambdaQueryWrapper<KnowledgeEntry> translationW = new LambdaQueryWrapper<KnowledgeEntry>().eq(KnowledgeEntry::getLibrary, "translation");
        LambdaQueryWrapper<KnowledgeEntry> chartW = new LambdaQueryWrapper<KnowledgeEntry>().eq(KnowledgeEntry::getLibrary, "chart");

        if (pid != null) {
            reportW.eq(KnowledgeEntry::getProjectId, pid);
            dynamicW.eq(KnowledgeEntry::getProjectId, pid);
            translationW.eq(KnowledgeEntry::getProjectId, pid);
            chartW.eq(KnowledgeEntry::getProjectId, pid);
        }

        long report = knowledgeEntryMapper.selectCount(reportW);
        long dynamic = knowledgeEntryMapper.selectCount(dynamicW);
        long translation = knowledgeEntryMapper.selectCount(translationW);
        long chart = knowledgeEntryMapper.selectCount(chartW);
        return Map.of("report", report, "dynamic", dynamic, "translation", translation, "chart", chart);
    }

    @GetMapping("/{id}")
    public KnowledgeEntry get(@PathVariable Long id) {
        return knowledgeEntryMapper.selectById(id);
    }

    /**
     * 手动创建词条（后台管理员）
     */
    @PostMapping("/")
    public Map<String, Object> create(@RequestBody KnowledgeEntry entry) {
        knowledgeEntryMapper.insert(entry);
        syncKG();
        return Map.of("id", entry.getId(), "message", "创建成功");
    }

    /**
     * 审核词条（后台管理员）
     */
    @PutMapping("/{id}/review")
    public Map<String, Object> review(@PathVariable Long id,
                                      @RequestParam String status,
                                      @RequestParam(required = false) String reviewer) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setId(id);
        entry.setStatus(status);
        entry.setReviewer(reviewer);
        knowledgeEntryMapper.updateById(entry);
        syncKG();
        return Map.of("message", "审核完成");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody KnowledgeEntry entry) {
        entry.setId(id);
        knowledgeEntryMapper.updateById(entry);
        syncKG();
        return Map.of("message", "更新成功");
    }

    /**
     * 更新词条的表格Markdown内容
     */
    @PutMapping("/{id}/table-markdown")
    public Map<String, Object> updateTableMarkdown(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String tableMarkdown = body.get("tableMarkdown");
        if (tableMarkdown == null) {
            return Map.of("status", "error", "message", "tableMarkdown is required");
        }

        KnowledgeEntry entry = knowledgeEntryMapper.selectById(id);
        if (entry == null) {
            return Map.of("status", "error", "message", "Entry not found");
        }

        entry.setTableMarkdown(tableMarkdown);
        knowledgeEntryMapper.updateById(entry);
        syncKG();

        // 重新索引向量搜索，确保编辑后的表格内容可被 Q&A 检索
        try {
            vectorSearchService.indexEntry(entry);
        } catch (Exception e) {
            // 索引失败不影响数据库更新结果，仅记录警告
            // 可通过重建索引修复
        }

        return Map.of("status", "success", "entry", entry);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        knowledgeEntryMapper.deleteById(id);
        syncKG();
        return Map.of("message", "删除成功");
    }

    /**
     * 批量审核
     */
    @PutMapping("/batch-review")
    public Map<String, Object> batchReview(@RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String reviewer = (String) body.get("reviewer");
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        for (Number idNum : ids) {
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.setId(idNum.longValue());
            entry.setStatus(status);
            entry.setReviewer(reviewer);
            knowledgeEntryMapper.updateById(entry);
        }
        syncKG();
        return Map.of("message", "批量审核完成，共" + ids.size() + "条");
    }

    private void syncKG() {
        try {
            kgController.buildGraph();
        } catch (Exception e) {
            // ignore
        }
    }
}
