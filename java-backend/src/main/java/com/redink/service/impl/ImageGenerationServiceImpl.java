package com.redink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redink.config.ConfigManager;
import com.redink.model.TaskState;
import com.redink.service.ImageGenerationService;
import com.redink.util.ImageUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.redink.util.ImageUtils.compressImage;

/**
 * 图片生成服务实现
 * 使用OpenAI DALL-E模型生成图片
 */
@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationServiceImpl.class);

    private final ConfigManager configManager;
    private final ImageModel openAiImageModel;

    private final ExecutorService executorService = Executors.newFixedThreadPool(15);
    private final Map<String, TaskState> taskStates = new ConcurrentHashMap<>();

    // 默认宽高比
    private static final String DEFAULT_ASPECT_RATIO = "3:4";

    public ImageGenerationServiceImpl(ConfigManager configManager,
                                     OpenAiImageModel openAiImageModel) {
        this.configManager = configManager;
        this.openAiImageModel = openAiImageModel;
    }
    
    @Override
    public SseEmitter generateImages(List<com.redink.model.Page> pages, String taskId, 
                                    String fullOutline, String userTopic, byte[][] userImages) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                executeImageGeneration(pages, taskId, fullOutline, userTopic, userImages, emitter);
            } catch (Exception e) {
                logger.error("图片生成任务失败: taskId={}", taskId, e);
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    logger.warn("SSE发射器完成失败", ex);
                }
            }
        }, executorService);
        
        return emitter;
    }
    
    private void executeImageGeneration(List<com.redink.model.Page> pages, String taskId,
                                       String fullOutline, String userTopic, byte[][] userImages,
                                       SseEmitter emitter) {
        Path taskDir = null;
        TaskState taskState = new TaskState();
        taskState.setGenerated(new HashMap<>());
        taskState.setFailed(new HashMap<>());
        taskStates.put(taskId, taskState);

        try {
            // 创建任务目录
            taskDir = Paths.get("history", taskId);
            Files.createDirectories(taskDir);
            logger.info("创建任务目录: {}", taskDir);

            // 发送开始事件
            sendSseEvent(emitter, "progress", createProgressData(0, pages.size(), "开始生成图片..."));

            int completed = 0;
            int failed = 0;
            List<Integer> failedIndices = new ArrayList<>();

            // 逐页生成图片
            for (int i = 0; i < pages.size(); i++) {
                com.redink.model.Page page = pages.get(i);
                try {
                    logger.info("开始生成第 {} 张图片, taskId={}", i + 1, taskId);

                    // 发送进度更新
                    sendSseEvent(emitter, "progress",
                        createProgressData(completed, pages.size(), "正在生成第 " + (i + 1) + " 张图片..."));

                    // 生成单张图片
                    byte[] referenceImage = (userImages != null && userImages.length > 0)
                        ? userImages[Math.min(i, userImages.length - 1)]
                        : null;

                    GenerateResult result = generateSingleImage(page, taskId, referenceImage, fullOutline, userTopic, userImages);

                    if (result.success && result.filename != null) {
                        // 生成成功
                        completed++;
                        taskState.getGenerated().put(i, result.filename);
                        logger.info("第 {} 张图片生成成功", i + 1);

                        // 发送单张完成事件
                        sendSseEvent(emitter, "imageCompleted", Map.of(
                            "index", i,
                            "filename", result.filename,
                            "url", "/api/images/" + taskId + "/" + result.filename
                        ));
                    } else {
                        // 生成失败
                        failed++;
                        failedIndices.add(i);
                        taskState.getFailed().put(i, result.error);
                        logger.warn("第 {} 张图片生成失败: {}", i + 1, result.error);

                        // 发送单张失败事件
                        sendSseEvent(emitter, "imageFailed", Map.of(
                            "index", i,
                            "error", result.error
                        ));
                    }

                } catch (Exception e) {
                    failed++;
                    failedIndices.add(i);
                    taskState.getFailed().put(i, e.getMessage());
                    logger.error("第 {} 张图片生成异常", i + 1, e);

                    sendSseEvent(emitter, "imageFailed", Map.of(
                        "index", i,
                        "error", e.getMessage()
                    ));
                }
            }

            // 发送完成事件
            boolean success = failed == 0;
            sendSseEvent(emitter, "finish", Map.of(
                "success", success,
                "taskId", taskId,
                "total", pages.size(),
                "completed", completed,
                "failed", failed,
                "failedIndices", failedIndices,
                "message", success ? "所有图片生成完成" : ("生成完成，失败 " + failed + " 张")
            ));

            logger.info("图片生成任务完成: taskId={}, total={}, success={}, failed={}",
                taskId, pages.size(), completed, failed);

        } catch (Exception e) {
            logger.error("图片生成任务异常: taskId={}", taskId, e);
            try {
                sendSseEvent(emitter, "error", Map.of("message", "生成失败: " + e.getMessage()));
            } catch (IOException ex) {
                logger.warn("发送SSE事件失败", ex);
            }
        } finally {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.warn("SSE发射器完成失败", e);
            }
        }
    }
    
    @Override
    public Map<String, Object> retrySingleImage(String taskId, com.redink.model.Page page,
                                              boolean useReference, String fullOutline, String userTopic) {
        try {
            logger.info("重试生成图片: taskId={}, page={}", taskId, page.getIndex());

            GenerateResult result = generateSingleImage(page, taskId, null, fullOutline, userTopic, null);

            if (result.success && result.filename != null) {
                return Map.of(
                    "success", true,
                    "index", page.getIndex(),
                    "filename", result.filename,
                    "url", "/api/images/" + taskId + "/" + result.filename
                );
            } else {
                return Map.of(
                    "success", false,
                    "index", page.getIndex(),
                    "error", result.error,
                    "retryable", true
                );
            }
        } catch (Exception e) {
            logger.error("重试图片生成失败", e);
            return Map.of(
                "success", false,
                "index", page.getIndex(),
                "error", e.getMessage(),
                "retryable", true
            );
        }
    }

    @Override
    public Map<String, Object> regenerateImage(String taskId, com.redink.model.Page page,
                                             boolean useReference, String fullOutline, String userTopic) {
        return retrySingleImage(taskId, page, useReference, fullOutline, userTopic);
    }
    
    @Override
    public TaskState getTaskState(String taskId) {
        return taskStates.get(taskId);
    }
    
    @Override
    public void cleanupTask(String taskId) {
        taskStates.remove(taskId);
    }
    
    /**
     * 生成单张图片
     */
    private GenerateResult generateSingleImage(com.redink.model.Page page, String taskId,
                                              byte[] referenceImage, String fullOutline,
                                              String userTopic, byte[][] userImages) {
        try {
            // 构建提示词
            String prompt = buildPrompt(page, fullOutline, userTopic);

            logger.info("生成图片提示词: {}", prompt);

            // 创建图片提示（使用默认选项）
            ImageOptions imageOptions = OpenAiImageOptions.builder()
                    .quality("hd")
                    .height(1024)
                    .width(768).build();
            ImagePrompt imagePrompt = new ImagePrompt(prompt, imageOptions);

            // 调用OpenAI图片生成API
            var response = openAiImageModel.call(imagePrompt);

            if (response.getResults().isEmpty()) {
                return new GenerateResult(page.getIndex(), false, null, "未收到图片生成结果");
            }

            // 获取生成的图片数据
            byte[] imageData = response.getResult().getOutput().getB64Json() != null
                ? ImageUtils.base64ToImage(response.getResult().getOutput().getB64Json())
                : response.getResult().getOutput().getUrl() != null
                    ? ImageUtils.downloadImage(response.getResult().getOutput().getUrl())
                    : null;

            if (imageData == null || imageData.length == 0) {
                return new GenerateResult(page.getIndex(), false, null, "图片数据为空");
            }

            // 保存图片
            String filename = String.format("page_%03d.png", page.getIndex());
            Path imagePath = Paths.get("history", taskId, filename);
            Files.write(imagePath, imageData);

            // 生成缩略图
            generateThumbnail(imageData, filename, imagePath.getParent());

            logger.info("图片生成并保存成功: {}, 大小={} bytes", filename, imageData.length);

            return new GenerateResult(page.getIndex(), true, filename, null);

        } catch (Exception e) {
            logger.error("图片生成失败", e);
            return new GenerateResult(page.getIndex(), false, null,
                "生成失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }
    
    /**
     * 生成缩略图
     */
    private void generateThumbnail(byte[] imageData, String filename, Path taskDir) {
        try {
            byte[] thumbnailData = compressImage(imageData, 50); // 50KB缩略图
            String thumbnailFilename = "thumb_" + filename;
            Path thumbnailPath = Paths.get(taskDir.toString(), thumbnailFilename);
            Files.write(thumbnailPath, thumbnailData);
        } catch (Exception e) {
            logger.warn("生成缩略图失败: {}", filename, e);
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(com.redink.model.Page page, String fullOutline, String userTopic) {
        return String.format("请生成一张小红书风格的图文内容图片。页面内容：%s，页面类型：%s，用户需求：%s", 
            page.getContent(), page.getType(), 
            userTopic != null ? userTopic : "未提供");
    }
    
    /**
     * 压缩用户图片
     */
    private byte[][] compressUserImages(byte[][] userImages) {
        if (userImages == null) return null;
        
        byte[][] compressed = new byte[userImages.length][];
        for (int i = 0; i < userImages.length; i++) {
            compressed[i] = compressImage(userImages[i], 200);
        }
        return compressed;
    }
    
    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event)
                .data(data));
    }
    
    private Map<String, Object> createProgressData(int current, int total, String message) {
        return Map.of("current", current, "total", total, "message", message);
    }
    
    /**
     * 图片生成结果
     */
    private static class GenerateResult {
        final int index;
        final boolean success;
        final String filename;
        final String error;
        
        GenerateResult(int index, boolean success, String filename, String error) {
            this.index = index;
            this.success = success;
            this.filename = filename;
            this.error = error;
        }
    }
}