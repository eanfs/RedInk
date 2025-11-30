package com.redink.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 大纲生成结果
 */
@Data
@Accessors(chain = true)
public class OutlineRequest {
    private String topic;
}