package com.redink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redink.config.ConfigManager;
import com.redink.model.TaskState;
import com.redink.service.ImageGenerationService;
import com.redink.util.ImageUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiImageModel;
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
 * 图片生成服务实现（简化版）
 * 暂时返回错误提示，等待AI服务配置
 */
@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationServiceImpl.class);
    
    private final ConfigManager configManager;
    private final OpenAiImageModel openAiImageModel;
    
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
        try {
            // 发送开始事件
            sendSseEvent(emitter, "progress", createProgressData(0, pages.size(), "AI服务暂时不可用"));
            
            // 发送完成事件，提示需要配置AI服务
            sendSseEvent(emitter, "finish", Map.of(
                "success", false,
                "taskId", taskId,
                "total", pages.size(),
                "completed", 0,
                "failed", pages.size(),
                "failedIndices", new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5)),
                "error", "AI图片生成服务未配置。请设置 OPENAI_API_KEY 环境变量以启用 OpenAI 图片生成功能。"
            ));
            
        } catch (Exception e) {
            logger.error("图片生成异常", e);
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
        return Map.of(
            "success", false,
            "index", page.getIndex(),
            "error", "AI服务不可用，请配置 OPENAI_API_KEY",
            "retryable", true
        );
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
     * 生成单张图片（简化版）
     */
    private GenerateResult generateSingleImage(com.redink.model.Page page, String taskId,
                                              byte[] referenceImage, String fullOutline, 
                                              String userTopic, byte[][] userImages) {
        return new GenerateResult(page.getIndex(), false, null, "AI服务不可用");
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