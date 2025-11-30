package com.redink.service;

import com.redink.model.TaskState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 图片生成服务接口
 */
public interface ImageGenerationService {
    
    /**
     * 生成图片
     * @param pages 页面列表
     * @param taskId 任务ID
     * @param fullOutline 完整大纲
     * @param userTopic 用户主题
     * @param userImages 用户图片
     * @return SSE发射器
     */
    SseEmitter generateImages(List<com.redink.model.Page> pages, String taskId, 
                              String fullOutline, String userTopic, byte[][] userImages);
    
    /**
     * 重试生成单张图片
     * @param taskId 任务ID
     * @param page 页面数据
     * @param useReference 是否使用参考图
     * @param fullOutline 完整大纲
     * @param userTopic 用户主题
     * @return 生成结果
     */
    Map<String, Object> retrySingleImage(String taskId, com.redink.model.Page page, 
                                        boolean useReference, String fullOutline, String userTopic);
    
    /**
     * 重新生成图片
     * @param taskId 任务ID
     * @param page 页面数据
     * @param useReference 是否使用参考图
     * @param fullOutline 完整大纲
     * @param userTopic 用户主题
     * @return 生成结果
     */
    Map<String, Object> regenerateImage(String taskId, com.redink.model.Page page,
                                      boolean useReference, String fullOutline, String userTopic);
    
    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskState getTaskState(String taskId);
    
    /**
     * 清理任务状态
     * @param taskId 任务ID
     */
    void cleanupTask(String taskId);
}