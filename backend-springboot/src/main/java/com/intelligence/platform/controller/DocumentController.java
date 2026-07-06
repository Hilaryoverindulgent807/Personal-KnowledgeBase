package com.intelligence.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.intelligence.platform.common.PageResult;
import com.intelligence.platform.entity.Document;
import com.intelligence.platform.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private com.intelligence.platform.mapper.KnowledgeEntryMapper knowledgeEntryMapper;

    @Autowired
    private com.intelligence.platform.service.DocumentParseService documentParseService;

    @Autowired
    private com.intelligence.platform.service.ProjectContext projectContext;

    @Autowired
    private com.intelligence.platform.mapper.ProjectMapper projectMapper;

    @GetMapping
    public PageResult<Document> listDocuments(
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String categoryL1,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        // 项目隔离
        Long projectId = projectContext.getCurrentProjectId();
        if (projectId != null) wrapper.eq(Document::getProjectId, projectId);
        if (docType != null && !docType.isEmpty()) wrapper.eq(Document::getDocType, docType);
        if (categoryL1 != null && !categoryL1.isEmpty()) wrapper.eq(Document::getCategoryL1, categoryL1);
        if (status != null && !status.isEmpty()) wrapper.eq(Document::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Document::getTitle, keyword).or().like(Document::getKeywords, keyword));
        }
        wrapper.orderByDesc(Document::getUploadTime);

        Page<Document> pageObj = new Page<>(page, pageSize);
        Page<Document> result = documentMapper.selectPage(pageObj, wrapper);
        // SQLite 不支持 COUNT(*) 在 selectPage 中，使用 items.size() 作为 fallback
        long total = result.getTotal() > 0 ? result.getTotal() : result.getRecords().size();
        return new PageResult<>(total, page, pageSize, result.getRecords());
    }

    @GetMapping("/{id}")
    public Document getDocument(@PathVariable Long id) {
        return documentMapper.selectById(id);
    }

    @PostMapping("/")
    public Map<String, Object> createDocument(@RequestBody Document doc) {
        documentMapper.insert(doc);
        return Map.of("id", doc.getId(), "message", "创建成功");
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateDocument(@PathVariable Long id, @RequestBody Document doc) {
        doc.setId(id);
        documentMapper.updateById(doc);
        return Map.of("message", "更新成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteDocument(@PathVariable Long id) {
        documentMapper.deleteById(id);
        return Map.of("message", "删除成功");
    }

    /**
     * 重新抽取文档的知识词条（删除旧词条后重新调用LLM抽取）
     */
    @PostMapping("/{id}/re-extract")
    public Map<String, Object> reExtract(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return Map.of("status", "error", "message", "文档不存在");
        }

        // 删除旧的失败词条
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.intelligence.platform.entity.KnowledgeEntry> deleteWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        deleteWrapper.eq(com.intelligence.platform.entity.KnowledgeEntry::getDocumentId, id);
        int deleted = 0;
        try {
            deleted = knowledgeEntryMapper.delete(deleteWrapper);
        } catch (Exception e) {
            // table may not exist yet
        }

        // 重新抽取
        try {
            java.util.List<com.intelligence.platform.entity.KnowledgeEntry> entries =
                    documentParseService.parseAndExtract(id);
            return Map.of(
                    "status", "success",
                    "message", "重新抽取完成",
                    "entry_count", entries.size(),
                    "deleted_old", deleted
            );
        } catch (Exception e) {
            return Map.of("status", "error", "message", "抽取失败: " + e.getMessage());
        }
    }

    /**
     * 批量重命名图表文档：将已有 docType=chart 的文档标题更新为新格式
     * 新格式：{项目名}_{图片/表格}_{序号}.{扩展名}
     */
    @PatchMapping("/batch-rename-charts")
    public Map<String, Object> batchRenameCharts() {
        Long projectId = projectContext.getCurrentProjectId();

        // 查询当前项目下所有 docType=chart 的文档
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) wrapper.eq(Document::getProjectId, projectId);
        wrapper.eq(Document::getDocType, "chart");
        wrapper.orderByAsc(Document::getUploadTime);
        List<Document> charts = documentMapper.selectList(wrapper);

        // 获取项目名
        String projectName = "未命名项目";
        if (projectId != null) {
            com.intelligence.platform.entity.Project project = projectMapper.selectById(projectId);
            if (project != null) {
                projectName = project.getName();
            }
        }

        int updatedCount = 0;
        for (int i = 0; i < charts.size(); i++) {
            Document doc = charts.get(i);
            long seq = i + 1;

            // 根据 categoryL1 判断"图片"或"表格"
            String label = "图片"; // 默认
            if (doc.getCategoryL1() != null) {
                String cat = doc.getCategoryL1().toLowerCase();
                if (cat.contains("表") || cat.equals("table")) {
                    label = "表格";
                }
            }

            // 从 filePath 或 sourcePath 提取扩展名
            String ext = "";
            String path = doc.getFilePath() != null ? doc.getFilePath() : doc.getSourcePath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot > 0) {
                    ext = path.substring(lastDot + 1).toLowerCase();
                }
            }

            String newTitle = projectName + "_" + label + "_" + seq;
            if (!ext.isEmpty()) {
                newTitle = newTitle + "." + ext;
            }

            doc.setTitle(newTitle);
            documentMapper.updateById(doc);
            updatedCount++;
        }

        return Map.of(
                "status", "success",
                "updated_count", updatedCount,
                "message", "批量重命名完成，共更新 " + updatedCount + " 个文档"
        );
    }
}
