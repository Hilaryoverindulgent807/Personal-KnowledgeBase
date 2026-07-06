package com.intelligence.platform.service;

import com.intelligence.platform.entity.KnowledgeEntry;
import com.intelligence.platform.controller.KGController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadTaskService {

    private static final Logger log = LoggerFactory.getLogger(UploadTaskService.class);

    public record UploadTask(
            Long docId,
            String filename,
            String fileTypeHint,
            String status,      // queued / processing / completed / failed / cancelled
            int progress,       // 0-100
            String message,
            int entryCount,
            Instant createdAt,
            String error
    ) {}

    private final ConcurrentHashMap<Long, UploadTask> tasks = new ConcurrentHashMap<>();

    @Autowired
    private DocumentParseService documentParseService;

    @Autowired
    private KGController kgController;

    public void submit(Long docId, String filename, String fileTypeHint) {
        tasks.put(docId, new UploadTask(
                docId, filename, fileTypeHint,
                "queued", 0, "等待处理", 0,
                Instant.now(), null
        ));
        log.info("Upload task submitted: docId={}, filename={}", docId, filename);
    }

    public void cancel(Long docId) {
        UploadTask task = tasks.get(docId);
        if (task != null && !task.status().equals("completed")) {
            tasks.put(docId, new UploadTask(
                    task.docId(), task.filename(), task.fileTypeHint(),
                    "cancelled", task.progress(), "已取消", task.entryCount(),
                    task.createdAt(), null
            ));
            log.info("Upload task cancelled: docId={}", docId);
        }
    }

    public Map<String, Object> getStatus(Long docId) {
        UploadTask task = tasks.get(docId);
        if (task == null) {
            return Map.of("docId", docId, "status", "unknown", "message", "任务不存在");
        }
        return taskToMap(task);
    }

    public List<Map<String, Object>> getAllTasks() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minutes
        List<Map<String, Object>> result = new ArrayList<>();
        for (UploadTask task : tasks.values()) {
            // Skip completed/failed/cancelled tasks older than 5 min
            if ((task.status().equals("completed") || task.status().equals("failed") || task.status().equals("cancelled"))
                    && task.createdAt().isBefore(cutoff)) {
                continue;
            }
            result.add(taskToMap(task));
        }
        return result;
    }

    public void cleanupOldTasks() {
        Instant cutoff = Instant.now().minusSeconds(300);
        tasks.entrySet().removeIf(entry -> {
            UploadTask task = entry.getValue();
            return (task.status().equals("completed") || task.status().equals("failed") || task.status().equals("cancelled"))
                    && task.createdAt().isBefore(cutoff);
        });
    }

    @Scheduled(fixedDelay = 500)
    public void processQueue() {
        // Find oldest queued task
        UploadTask queuedTask = null;
        for (UploadTask task : tasks.values()) {
            if (task.status().equals("queued")) {
                if (queuedTask == null || task.createdAt().isBefore(queuedTask.createdAt())) {
                    queuedTask = task;
                }
            }
        }

        if (queuedTask == null) return;

        Long docId = queuedTask.docId();

        // Set to processing
        tasks.put(docId, new UploadTask(
                queuedTask.docId(), queuedTask.filename(), queuedTask.fileTypeHint(),
                "processing", 10, "正在解析文档", 0,
                queuedTask.createdAt(), null
        ));

        try {
            // Check cancellation
            if (isCancelled(docId)) return;

            // Update progress
            updateProgress(docId, 30, "正在调用LLM抽取词条");

            List<KnowledgeEntry> result = documentParseService.parseAndExtract(docId, queuedTask.fileTypeHint());

            // Check cancellation again
            if (isCancelled(docId)) return;

            // Success
            tasks.put(docId, new UploadTask(
                    queuedTask.docId(), queuedTask.filename(), queuedTask.fileTypeHint(),
                    "completed", 100, "处理完成", result.size(),
                    queuedTask.createdAt(), null
            ));
            
            // Rebuild knowledge graph to keep it in sync with newly extracted entries
            try {
                kgController.buildGraph();
            } catch (Exception e) {
                log.warn("Auto-building KG graph failed after upload task completed: docId={}, error={}", docId, e.getMessage());
            }
            
            log.info("Upload task completed: docId={}, entries={}", docId, result.size());

        } catch (Exception e) {
            log.error("Upload task failed: docId={}", docId, e);
            tasks.put(docId, new UploadTask(
                    queuedTask.docId(), queuedTask.filename(), queuedTask.fileTypeHint(),
                    "failed", 0, "处理失败: " + e.getMessage(), 0,
                    queuedTask.createdAt(), e.getMessage()
            ));
        }
    }

    private boolean isCancelled(Long docId) {
        UploadTask task = tasks.get(docId);
        return task != null && task.status().equals("cancelled");
    }

    private void updateProgress(Long docId, int progress, String message) {
        UploadTask task = tasks.get(docId);
        if (task != null) {
            tasks.put(docId, new UploadTask(
                    task.docId(), task.filename(), task.fileTypeHint(),
                    task.status(), progress, message, task.entryCount(),
                    task.createdAt(), task.error()
            ));
        }
    }

    private Map<String, Object> taskToMap(UploadTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("docId", task.docId());
        map.put("filename", task.filename());
        map.put("status", task.status());
        map.put("progress", task.progress());
        map.put("message", task.message());
        map.put("entryCount", task.entryCount());
        map.put("createdAt", task.createdAt().toString());
        if (task.error() != null) {
            map.put("error", task.error());
        }
        return map;
    }
}
