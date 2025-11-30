package com.redink.controller;

import com.redink.config.ConfigManager;
import com.redink.model.*;
import com.redink.service.HistoryService;
import com.redink.service.ImageGenerationService;
import com.redink.service.OutlineGenerationService;
import com.redink.util.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 主要API控制器
 * 处理大纲生成、图片生成、历史记录等核心功能
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final OutlineGenerationService outlineService;
    private final ImageGenerationService imageService;
    private final HistoryService historyService;
    private final ConfigManager configManager;

    public ApiController(OutlineGenerationService outlineService,
                         ImageGenerationService imageService,
                         HistoryService historyService,
                         ConfigManager configManager) {
        this.outlineService = outlineService;
        this.imageService = imageService;
        this.historyService = historyService;
        this.configManager = configManager;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("服务正常运行", "服务状态正常"));
    }

    /**
     * 生成大纲
     */
    @PostMapping(value = "/outline")
    public ResponseEntity<OutlineResult> generateOutline(
            @RequestBody OutlineRequest topic,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        logger.info("开始生成大纲: topic={}, images={}", topic, images != null ? images.length : 0);

        byte[][] imageData = null;
        if (images != null && images.length > 0) {
            imageData = new byte[images.length][];
            for (int i = 0; i < images.length; i++) {
                try {
                    imageData[i] = images[i].getBytes();
                } catch (IOException e) {
                    logger.warn("读取图片失败: index={}", i, e);
                }
            }
        }

        OutlineResult result = outlineService.generateOutline(topic.getTopic(), imageData);

        if (result.isSuccess()) {
            logger.info("大纲生成成功: 主题={}, 页数={}", topic, result.getPages().size());
            return ResponseEntity.ok(result);
        } else {
            logger.error("大纲生成失败: {}", result.getError());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }

    }

    /**
     * 生成图片（SSE流式响应）
     */
    @PostMapping("/generate")
    public SseEmitter generateImages(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pages = (List<Map<String, Object>>) request.get("pages");
            String taskId = (String) request.getOrDefault("taskId", UUID.randomUUID().toString());
            String fullOutline = (String) request.get("fullOutline");
            String userTopic = (String) request.get("userTopic");

            @SuppressWarnings("unchecked")
            List<String> userImagesBase64 = (List<String>) request.get("userImages");
            byte[][] userImages = null;

            if (userImagesBase64 != null && !userImagesBase64.isEmpty()) {
                userImages = new byte[userImagesBase64.size()][];
                for (int i = 0; i < userImagesBase64.size(); i++) {
                    try {
                        userImages[i] = ImageUtils.base64ToImage(userImagesBase64.get(i));
                    } catch (Exception e) {
                        logger.warn("解析用户图片失败: index={}", i, e);
                        userImages[i] = null;
                    }
                }
            }

            @SuppressWarnings("unchecked")
            List<Page> pageObjects = pages.stream()
                    .map(pageMap -> {
                        Page page = new Page();
                        page.setIndex((Integer) pageMap.get("index"));
                        page.setType((String) pageMap.get("type"));
                        page.setContent((String) pageMap.get("content"));
                        return page;
                    })
                    .toList();

            logger.info("开始图片生成任务: taskId={}, pages={}", taskId, pageObjects.size());

            return imageService.generateImages(pageObjects, taskId, fullOutline, userTopic, userImages);

        } catch (Exception e) {
            logger.error("图片生成请求异常", e);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data(Map.of("error", "请求异常: " + e.getMessage())));
                emitter.complete();
            } catch (IOException ex) {
                logger.warn("发送SSE事件失败", ex);
            }
            return emitter;
        }
    }

    /**
     * 获取图片
     */
    @GetMapping("/images/{taskId}/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String taskId,
                                           @PathVariable String filename,
                                           @RequestParam(defaultValue = "true") boolean thumbnail) {
        try {
            Path imagePath = Paths.get("history", taskId, filename);

            if (thumbnail) {
                Path thumbPath = Paths.get("history", taskId, "thumb_" + filename);
                if (Files.exists(thumbPath)) {
                    imagePath = thumbPath;
                }
            }

            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageData = Files.readAllBytes(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageData);

        } catch (IOException e) {
            logger.error("获取图片失败: {}/{}", taskId, filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 重试生成单张图片
     */
    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retrySingleImage(@RequestBody Map<String, Object> request) {
        try {
            String taskId = (String) request.get("taskId");
            @SuppressWarnings("unchecked")
            Map<String, Object> pageMap = (Map<String, Object>) request.get("page");
            Boolean useReference = (Boolean) request.getOrDefault("useReference", true);
            String fullOutline = (String) request.getOrDefault("fullOutline", "");
            String userTopic = (String) request.getOrDefault("userTopic", "");

            Page page = new Page();
            page.setIndex((Integer) pageMap.get("index"));
            page.setType((String) pageMap.get("type"));
            page.setContent((String) pageMap.get("content"));

            logger.info("重试生成图片: taskId={}, page={}", taskId, page.getIndex());

            Map<String, Object> result = imageService.retrySingleImage(taskId, page, useReference, fullOutline, userTopic);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            logger.error("重试图片生成异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("重试失败: " + e.getMessage()));
        }
    }

    /**
     * 重新生成图片
     */
    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerateImage(@RequestBody Map<String, Object> request) {
        try {
            String taskId = (String) request.get("taskId");
            @SuppressWarnings("unchecked")
            Map<String, Object> pageMap = (Map<String, Object>) request.get("page");
            Boolean useReference = (Boolean) request.getOrDefault("useReference", true);
            String fullOutline = (String) request.getOrDefault("fullOutline", "");
            String userTopic = (String) request.getOrDefault("userTopic", "");

            Page page = new Page();
            page.setIndex((Integer) pageMap.get("index"));
            page.setType((String) pageMap.get("type"));
            page.setContent((String) pageMap.get("content"));

            logger.info("重新生成图片: taskId={}, page={}", taskId, page.getIndex());

            Map<String, Object> result = imageService.regenerateImage(taskId, page, useReference, fullOutline, userTopic);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            logger.error("重新生成图片异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("重新生成失败: " + e.getMessage()));
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<TaskState>> getTaskState(@PathVariable String taskId) {
        try {
            TaskState state = imageService.getTaskState(taskId);

            if (state == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("任务不存在: " + taskId));
            }

            // 不返回封面图片数据（太大）
            TaskState safeState = new TaskState();
            safeState.setGenerated(state.getGenerated());
            safeState.setFailed(state.getFailed());
            safeState.setCoverImage(state.getCoverImage() != null ? new byte[1] : null);

            return ResponseEntity.ok(ApiResponse.success(safeState));

        } catch (Exception e) {
            logger.error("获取任务状态异常: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取任务状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统配置
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        try {
            Map<String, Object> imageConfig = configManager.loadImageProvidersConfig();
            Map<String, Object> textConfig = configManager.loadTextProvidersConfig();

            // 脱敏API密钥
            Map<String, Object> responseConfig = new HashMap<>();

            // 图片配置
            Map<String, Object> imageResponse = new HashMap<>();
            imageResponse.put("activeProvider", imageConfig.getOrDefault("active_provider", ""));

            @SuppressWarnings("unchecked")
            Map<String, Object> imageProviders = (Map<String, Object>) imageConfig.get("providers");
            Map<String, Object> maskedImageProviders = new HashMap<>();

            if (imageProviders != null) {
                for (Map.Entry<String, Object> entry : imageProviders.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> provider = (Map<String, Object>) entry.getValue();
                    Map<String, Object> maskedProvider = new HashMap<>(provider);

                    String apiKey = (String) provider.get("apiKey");
                    if (apiKey != null && !apiKey.trim().isEmpty()) {
                        maskedProvider.put("apiKeyMasked", maskApiKey(apiKey));
                        maskedProvider.put("apiKey", "");
                    }

                    maskedImageProviders.put(entry.getKey(), maskedProvider);
                }
            }

            imageResponse.put("providers", maskedImageProviders);
            responseConfig.put("imageGeneration", imageResponse);

            // 文本配置
            Map<String, Object> textResponse = new HashMap<>();
            textResponse.put("activeProvider", textConfig.getOrDefault("active_provider", ""));

            @SuppressWarnings("unchecked")
            Map<String, Object> textProviders = (Map<String, Object>) textConfig.get("providers");
            Map<String, Object> maskedTextProviders = new HashMap<>();

            if (textProviders != null) {
                for (Map.Entry<String, Object> entry : textProviders.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> provider = (Map<String, Object>) entry.getValue();
                    Map<String, Object> maskedProvider = new HashMap<>(provider);

                    String apiKey = (String) provider.get("apiKey");
                    if (apiKey != null && !apiKey.trim().isEmpty()) {
                        maskedProvider.put("apiKeyMasked", maskApiKey(apiKey));
                        maskedProvider.put("apiKey", "");
                    }

                    maskedTextProviders.put(entry.getKey(), maskedProvider);
                }
            }

            textResponse.put("providers", maskedTextProviders);
            responseConfig.put("textGeneration", textResponse);

            return ResponseEntity.ok(ApiResponse.success(responseConfig));

        } catch (Exception e) {
            logger.error("获取配置异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取配置失败: " + e.getMessage()));
        }
    }

    /**
     * 遮盖API密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "*".repeat(apiKey != null ? apiKey.length() : 0);
        }
        return apiKey.substring(0, 4) + "*".repeat(apiKey.length() - 8) + apiKey.substring(apiKey.length() - 4);
    }
}