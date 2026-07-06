package com.intelligence.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intelligence.platform.entity.LlmConfig;
import com.intelligence.platform.mapper.LlmConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * LLM调用服务
 * 参考 llm_wiki 的 llm-providers.ts / llm-client.ts
 * 支持国际提供商：OpenAI / Anthropic / Google / Ollama / Azure / Custom
 * 支持中国提供商：DeepSeek / Qwen / MiniMax / Moonshot / StepFun / Xiaomi / Hunyuan / Doubao
 */
@Service
public class LlmService {

    @Autowired
    private LlmConfigMapper llmConfigMapper;

    @Value("${llm.default.api-key:}")
    private String defaultApiKey;

    @Value("${llm.default.base-url:https://api.deepseek.com}")
    private String defaultBaseUrl;

    @Value("${llm.default.model:deepseek-chat}")
    private String defaultModel;

    @Value("${llm.default.provider:deepseek}")
    private String defaultProvider;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 获取当前启用的对话LLM配置
     * 数据库配置优先；若无活跃数据库配置，则从环境变量构建默认配置
     */
    public LlmConfig getActiveChatConfig() {
        List<LlmConfig> configs = llmConfigMapper.selectList(
                new LambdaQueryWrapper<LlmConfig>()
                        .eq(LlmConfig::getEnabled, true)
                        .in(LlmConfig::getPurpose, "chat", "both")
                        .orderByDesc(LlmConfig::getId)
                        .last("LIMIT 1"));
        if (!configs.isEmpty()) {
            return configs.get(0);
        }
        // 数据库无活跃配置，回退到环境变量默认值
        return buildDefaultConfig();
    }

    /**
     * 从环境变量构建默认LLM配置
     */
    private LlmConfig buildDefaultConfig() {
        if (defaultApiKey == null || defaultApiKey.isEmpty()) {
            return null;
        }
        LlmConfig config = new LlmConfig();
        config.setName("默认配置（环境变量）");
        config.setProvider(defaultProvider);
        config.setApiKey(defaultApiKey);
        config.setModel(defaultModel);
        config.setBaseUrl(defaultBaseUrl);
        config.setEnabled(true);
        config.setPurpose("chat");
        config.setMaxContextSize(4096);
        config.setApiMode("chat_completions");
        return config;
    }

    /**
     * 获取当前启用的Embedding配置
     */
    public LlmConfig getActiveEmbeddingConfig() {
        List<LlmConfig> configs = llmConfigMapper.selectList(
                new LambdaQueryWrapper<LlmConfig>()
                        .eq(LlmConfig::getEnabled, true)
                        .in(LlmConfig::getPurpose, "embedding", "both")
                        .orderByDesc(LlmConfig::getId)
                        .last("LIMIT 1"));
        return configs.isEmpty() ? null : configs.get(0);
    }

    /**
     * 获取当前启用的OCR配置（优先purpose=ocr，其次复用chat）
     */
    public LlmConfig getActiveOcrConfig() {
        // 优先查找 purpose=ocr 的配置
        List<LlmConfig> configs = llmConfigMapper.selectList(
                new LambdaQueryWrapper<LlmConfig>()
                        .eq(LlmConfig::getEnabled, true)
                        .eq(LlmConfig::getPurpose, "ocr")
                        .orderByDesc(LlmConfig::getId)
                        .last("LIMIT 1"));
        if (!configs.isEmpty()) {
            return configs.get(0);
        }
        // 回退到 chat 配置
        return getActiveChatConfig();
    }

    /**
     * 发送对话请求
     * @param config LLM配置
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return LLM回复文本
     */
    public String chat(LlmConfig config, String systemPrompt, String userMessage) throws Exception {
        String url = resolveChatUrl(config);
        String body = buildChatBody(config, systemPrompt, userMessage);
        String authHeader = buildAuthHeader(config);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (authHeader != null) {
            reqBuilder.header("Authorization", authHeader);
        }

        // Anthropic / custom+anthropic_messages 特殊头
        boolean isAnthropicWire = "anthropic".equals(config.getProvider())
                || ("custom".equals(config.getProvider()) && "anthropic_messages".equals(config.getApiMode()));
        if (isAnthropicWire && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            reqBuilder.header("x-api-key", config.getApiKey());
            reqBuilder.header("anthropic-version", "2023-06-01");
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            String errorBody = response.body();
            // 尝试解析 JSON 错误信息
            try {
                JsonNode err = mapper.readTree(errorBody);
                if (err.has("error")) {
                    JsonNode errorNode = err.get("error");
                    String msg = errorNode.has("message") ? errorNode.get("message").asText() : errorNode.asText();
                    throw new RuntimeException("API 错误 (" + response.statusCode() + "): " + msg);
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ignored) {
                // 非 JSON 响应
            }
            throw new RuntimeException("API 请求失败 (HTTP " + response.statusCode() + "): "
                    + (errorBody != null && errorBody.length() > 200 ? errorBody.substring(0, 200) : errorBody));
        }

        return parseChatResponse(config, response.body());
    }

    /**
     * 使用当前活跃配置发送对话
     */
    public String chatWithActive(String systemPrompt, String userMessage) throws Exception {
        LlmConfig config = getActiveChatConfig();
        if (config == null) {
            return "错误：未配置LLM。请在后台系统配置中添加并启用LLM。";
        }
        return chat(config, systemPrompt, userMessage);
    }

    private String resolveChatUrl(LlmConfig config) {
        // custom provider: 根据apiMode决定URL后缀
        if ("custom".equals(config.getProvider())) {
            String base = config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434";
            if ("anthropic_messages".equals(config.getApiMode())) {
                // Anthropic-compatible endpoint
                return base.endsWith("/messages") ? base : base + "/messages";
            }
            // OpenAI-compatible endpoint
            return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
        }

        // 中国LLM提供商（均使用OpenAI兼容API格式）
        return switch (config.getProvider()) {
            // 国际提供商
            case "openai" -> "https://api.openai.com/v1/chat/completions";
            case "anthropic" -> "https://api.anthropic.com/v1/messages";
            case "google" -> "https://generativelanguage.googleapis.com/v1beta/models/"
                    + config.getModel() + ":generateContent?key=" + config.getApiKey();
            case "ollama" -> {
                String base = config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434";
                yield base + "/v1/chat/completions";
            }
            case "azure" -> config.getBaseUrl();
            // 中国提供商 - DeepSeek（深度求索）
            case "deepseek" -> "https://api.deepseek.com/chat/completions";
            // 中国提供商 - Qwen（通义千问 - 阿里云）
            case "qwen" -> {
                String base = config.getBaseUrl() != null ? config.getBaseUrl() : "https://dashscope.aliyuncs.com/compatible-mode/v1";
                yield base + "/chat/completions";
            }
            // 中国提供商 - MiniMax
            case "minimax" -> "https://api.minimax.chat/v1/text/chatcompletion_v2";
            // 中国提供商 - Moonshot（月之暗面）
            case "moonshot" -> "https://api.moonshot.cn/v1/chat/completions";
            // 中国提供商 - StepFun（阶跃星辰）
            case "stepfun" -> "https://api.stepfun.com/v1/chat/completions";
            // 中国提供商 - Xiaomi（小米）
            case "xiaomi" -> "https://api.xiaomi.com/v1/chat/completions";
            // 中国提供商 - Hunyuan（混元 - 腾讯）
            case "hunyuan" -> "https://api.hunyuan.cloud.tencent.com/v1/chat/completions";
            // 中国提供商 - Doubao（豆包 - 字节跳动/火山引擎）
            case "doubao" -> {
                String base = config.getBaseUrl() != null ? config.getBaseUrl() : "https://ark.cn-beijing.volces.com/api/v3";
                yield base + "/chat/completions";
            }
            // 中国提供商 - SiliconFlow（硅基流动）
            case "siliconflow" -> {
                String base = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.siliconflow.cn/v1";
                yield base + "/chat/completions";
            }
            default -> {
                if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
                    yield config.getBaseUrl();
                }
                throw new RuntimeException("未配置 Base URL，请在设置中填写 API 地址");
            }
        };
    }

    private String buildChatBody(LlmConfig config, String systemPrompt, String userMessage) throws Exception {
        // Anthropic原生 或 custom+anthropic_messages模式（如MiniMax）
        if ("anthropic".equals(config.getProvider())
                || ("custom".equals(config.getProvider()) && "anthropic_messages".equals(config.getApiMode()))) {
            ObjectNode root = mapper.createObjectNode();
            root.put("model", config.getModel());
            root.put("max_tokens", 4096);
            if (systemPrompt != null) root.put("system", systemPrompt);
            ArrayNode messages = root.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userMessage);
            return mapper.writeValueAsString(root);
        }

        if ("google".equals(config.getProvider())) {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode contents = root.putArray("contents").addObject();
            contents.put("role", "user");
            contents.putArray("parts").addObject().put("text",
                    (systemPrompt != null ? systemPrompt + "\n\n" : "") + userMessage);
            return mapper.writeValueAsString(root);
        }

        // OpenAI / Ollama / Azure / Custom / 中国提供商（均使用OpenAI兼容格式）
        ObjectNode root = mapper.createObjectNode();
        root.put("model", config.getModel());
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userMessage);
        root.put("max_tokens", 4096);
        return mapper.writeValueAsString(root);
    }

    private String buildAuthHeader(LlmConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) return null;
        // Anthropic原生 或 custom+anthropic_messages → 使用 x-api-key
        if ("anthropic".equals(config.getProvider())
                || ("custom".equals(config.getProvider()) && "anthropic_messages".equals(config.getApiMode()))) {
            return null; // 使用 x-api-key header
        }
        return switch (config.getProvider()) {
            case "google" -> null; // 使用 URL 参数
            default -> "Bearer " + config.getApiKey();
        };
    }

    private String parseChatResponse(LlmConfig config, String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);

        // Anthropic原生 或 custom+anthropic_messages
        if ("anthropic".equals(config.getProvider())
                || ("custom".equals(config.getProvider()) && "anthropic_messages".equals(config.getApiMode()))) {
            JsonNode content = root.get("content");
            if (content != null && content.isArray() && !content.isEmpty()) {
                return content.get(0).get("text").asText();
            }
            return root.has("error") ? "错误: " + root.get("error").get("message").asText() : responseBody;
        }

        if ("google".equals(config.getProvider())) {
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).get("content").get("parts");
                return parts.get(0).get("text").asText();
            }
            return responseBody;
        }

        // OpenAI / Ollama / Azure / Custom / 中国提供商
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).get("message").get("content").asText();
        }
        if (root.has("error")) {
            return "错误: " + root.get("error").get("message").asText();
        }
        return responseBody;
    }

    /**
     * 发送带图片的视觉对话请求（Vision API）
     * @param config LLM配置
     * @param systemPrompt 系统提示词
     * @param userMessage 用户文本消息
     * @param imageBase64 图片的Base64编码
     * @param imageMimeType 图片MIME类型（如 image/png）
     * @return LLM回复文本
     */
    public String chatWithVision(LlmConfig config, String systemPrompt, String userMessage,
                                  String imageBase64, String imageMimeType) throws Exception {
        String url = resolveChatUrl(config);
        String body = buildVisionBody(config, systemPrompt, userMessage, imageBase64, imageMimeType);
        String authHeader = buildAuthHeader(config);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (authHeader != null) {
            reqBuilder.header("Authorization", authHeader);
        }

        boolean isAnthropicWire = "anthropic".equals(config.getProvider())
                || ("custom".equals(config.getProvider()) && "anthropic_messages".equals(config.getApiMode()));
        if (isAnthropicWire && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            reqBuilder.header("x-api-key", config.getApiKey());
            reqBuilder.header("anthropic-version", "2023-06-01");
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            String errorBody = response.body();
            try {
                JsonNode err = mapper.readTree(errorBody);
                if (err.has("error")) {
                    JsonNode errorNode = err.get("error");
                    String msg = errorNode.has("message") ? errorNode.get("message").asText() : errorNode.asText();
                    throw new RuntimeException("API 错误 (" + response.statusCode() + "): " + msg);
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ignored) {
            }
            throw new RuntimeException("API 请求失败 (HTTP " + response.statusCode() + "): "
                    + (errorBody != null && errorBody.length() > 200 ? errorBody.substring(0, 200) : errorBody));
        }

        return parseChatResponse(config, response.body());
    }

    private String buildVisionBody(LlmConfig config, String systemPrompt, String userMessage,
                                    String imageBase64, String imageMimeType) throws Exception {
        // Anthropic 原生格式
        if ("anthropic".equals(config.getProvider())
                || ("custom".equals(config.getProvider()) && "anthropic_messages".equals(config.getApiMode()))) {
            ObjectNode root = mapper.createObjectNode();
            root.put("model", config.getModel());
            root.put("max_tokens", 4096);
            if (systemPrompt != null) root.put("system", systemPrompt);
            ArrayNode messages = root.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            ArrayNode contentArr = msg.putArray("content");
            ObjectNode imageNode = contentArr.addObject();
            imageNode.put("type", "image");
            ObjectNode source = imageNode.putObject("source");
            source.put("type", "base64");
            source.put("media_type", imageMimeType);
            source.put("data", imageBase64);
            contentArr.addObject().put("type", "text").put("text", userMessage);
            return mapper.writeValueAsString(root);
        }

        // Google Gemini 格式
        if ("google".equals(config.getProvider())) {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode contents = root.putArray("contents").addObject();
            contents.put("role", "user");
            ArrayNode parts = contents.putArray("parts");
            ObjectNode inlineData = parts.addObject().putObject("inline_data");
            inlineData.put("mime_type", imageMimeType);
            inlineData.put("data", imageBase64);
            parts.addObject().put("text", (systemPrompt != null ? systemPrompt + "\n\n" : "") + userMessage);
            return mapper.writeValueAsString(root);
        }

        // OpenAI兼容格式（含中国提供商）
        ObjectNode root = mapper.createObjectNode();
        root.put("model", config.getModel());
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null) {
            messages.addObject().put("role", "system").put("content", systemPrompt);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode userContent = user.putArray("content");
        ObjectNode textPart = userContent.addObject();
        textPart.put("type", "text");
        textPart.put("text", userMessage);
        ObjectNode imagePart = userContent.addObject();
        imagePart.put("type", "image_url");
        imagePart.putObject("image_url")
                .put("url", "data:" + imageMimeType + ";base64," + imageBase64);
        root.put("max_tokens", 4096);
        return mapper.writeValueAsString(root);
    }

    /**
     * 获取所有支持的LLM提供商列表（供前端使用）
     */
    public java.util.List<java.util.Map<String, String>> getSupportedProviders() {
        return java.util.List.of(
                // 中国提供商
                java.util.Map.of("value", "deepseek", "label", "DeepSeek (深度求索)", "desc", "国产大模型，高性价比", "region", "cn"),
                java.util.Map.of("value", "qwen", "label", "Qwen (通义千问)", "desc", "阿里云大模型", "region", "cn"),
                java.util.Map.of("value", "minimax", "label", "MiniMax", "desc", "国内大模型平台", "region", "cn"),
                java.util.Map.of("value", "moonshot", "label", "Moonshot (月之暗面)", "desc", "Kimi大模型", "region", "cn"),
                java.util.Map.of("value", "stepfun", "label", "StepFun (阶跃星辰)", "desc", "国产大模型", "region", "cn"),
                java.util.Map.of("value", "xiaomi", "label", "Xiaomi (小米)", "desc", "小米大模型", "region", "cn"),
                java.util.Map.of("value", "hunyuan", "label", "Hunyuan (混元)", "desc", "腾讯大模型", "region", "cn"),
                java.util.Map.of("value", "doubao", "label", "Doubao (豆包)", "desc", "字节跳动/火山引擎大模型", "region", "cn"),
                // 国际提供商
                java.util.Map.of("value", "openai", "label", "OpenAI", "desc", "GPT系列模型", "region", "global"),
                java.util.Map.of("value", "anthropic", "label", "Anthropic", "desc", "Claude系列模型", "region", "global"),
                java.util.Map.of("value", "google", "label", "Google", "desc", "Gemini系列模型", "region", "global"),
                java.util.Map.of("value", "ollama", "label", "Ollama (本地)", "desc", "本地运行开源模型", "region", "local"),
                java.util.Map.of("value", "azure", "label", "Azure OpenAI", "desc", "微软Azure托管OpenAI", "region", "global"),
                java.util.Map.of("value", "custom", "label", "Custom (自定义)", "desc", "自定义API端点", "region", "custom")
        );
    }
}
