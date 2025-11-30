package com.redink.service;

import com.redink.model.OutlineResult;

/**
 * 大纲生成服务接口
 */
public interface OutlineGenerationService {
    
    /**
     * 生成大纲
     * @param topic 主题
     * @param images 图片数据
     * @return 大纲生成结果
     */
    OutlineResult generateOutline(String topic, byte[][] images);
}