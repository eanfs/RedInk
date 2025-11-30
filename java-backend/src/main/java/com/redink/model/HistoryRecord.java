package com.redink.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 历史记录模型
 */
@Data
public class HistoryRecord {
    private String id;
    private String title;
    private String createdAt;
    private String updatedAt;
    private OutlineResult outline;
    private ImagesInfo images;
    private String status; // draft/generating/completed/partial
    private String thumbnail;
    
    @Data
    public static class ImagesInfo {
        private String taskId;
        private String[] generated;
    }
}