package com.intelligence.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * LLM配置实体
 * 参考 llm_wiki 的 LLM Provider 配置模型
 */
@Data
@TableName("llm_configs")
public class LlmConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 配置名称 */
    private String name;
    /** 提供商：openai/anthropic/google/azure/ollama/custom */
    private String provider;
    /** API Key */
    private String apiKey;
    /** 模型名称 */
    private String model;
    /** Base URL (用于ollama/custom) */
    private String baseUrl;
    /** 是否启用 */
    private Boolean enabled;
    /** 用途：chat(对话)/embedding(向量)/both(两者) */
    private String purpose;
    /** 上下文窗口大小 */
    private Integer maxContextSize;
    /** 自定义API模式：chat_completions/anthropic_messages */
    private String apiMode;
    /** Azure API版本 */
    private String azureApiVersion;
    /** 创建时间 */
    private String createdAt;
    /** 备注 */
    private String description;
}
