package com.redink.service;

import com.redink.model.HistoryRecord;
import com.redink.model.OutlineResult;

/**
 * 历史记录服务接口
 */
public interface HistoryService {
    
    /**
     * 创建历史记录
     * @param topic 主题
     * @param outline 大纲结果
     * @param taskId 任务ID
     * @return 记录ID
     */
    String createRecord(String topic, OutlineResult outline, String taskId);
    
    /**
     * 获取历史记录
     * @param recordId 记录ID
     * @return 历史记录
     */
    HistoryRecord getRecord(String recordId);
    
    /**
     * 更新历史记录
     * @param recordId 记录ID
     * @param outline 大纲
     * @param images 图片信息
     * @param status 状态
     * @param thumbnail 缩略图
     * @return 是否成功
     */
    boolean updateRecord(String recordId, OutlineResult outline, HistoryRecord.ImagesInfo images, 
                        String status, String thumbnail);
    
    /**
     * 删除历史记录
     * @param recordId 记录ID
     * @return 是否成功
     */
    boolean deleteRecord(String recordId);
    
    /**
     * 获取历史记录列表
     * @param page 页码
     * @param pageSize 页大小
     * @param status 状态筛选
     * @return 分页结果
     */
    PagedResult<HistoryRecord> listRecords(int page, int pageSize, String status);
    
    /**
     * 搜索历史记录
     * @param keyword 关键词
     * @return 搜索结果
     */
    java.util.List<HistoryRecord> searchRecords(String keyword);
    
    /**
     * 获取统计信息
     * @return 统计数据
     */
    HistoryStats getStatistics();
}