package com.redink.model;

import lombok.Data;
import java.util.Map;

/**
 * 图片生成任务状态
 */
@Data
public class TaskState {
    private Map<Integer, String> generated; // index -> filename
    private Map<Integer, String> failed;    // index -> error message
    private byte[] coverImage;
    private String fullOutline;
    private String userTopic;
    private byte[][] userImages;
}