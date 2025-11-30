package com.redink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.redink.model.HistoryRecord;
import com.redink.model.OutlineResult;
import com.redink.service.HistoryService;
import com.redink.service.PagedResult;
import com.redink.service.HistoryStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 历史记录服务实现
 */
@Service
public class HistoryServiceImpl implements HistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(HistoryServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final Path historyDir = Paths.get("history");
    private final Path indexFile = historyDir.resolve("index.json");
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    // 内存索引缓存
    private volatile Map<String, Object> indexCache;
    
    public HistoryServiceImpl() {
        initializeHistoryDirectory();
    }
    
    @Override
    public String createRecord(String topic, OutlineResult outline, String taskId) {
        String recordId = UUID.randomUUID().toString();
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        
        HistoryRecord record = new HistoryRecord();
        record.setId(recordId);
        record.setTitle(topic);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.setOutline(outline);
        
        HistoryRecord.ImagesInfo imagesInfo = new HistoryRecord.ImagesInfo();
        imagesInfo.setTaskId(taskId);
        record.setImages(imagesInfo);
        
        record.setStatus("draft");
        
        // 保存记录
        saveRecord(record);
        
        // 更新索引
        updateIndex(record, "create");
        
        logger.info("创建历史记录: id={}, topic={}", recordId, topic);
        return recordId;
    }
    
    @Override
    public HistoryRecord getRecord(String recordId) {
        Path recordFile = historyDir.resolve(recordId + ".json");
        if (!Files.exists(recordFile)) {
            return null;
        }
        
        try {
            return objectMapper.readValue(recordFile.toFile(), HistoryRecord.class);
        } catch (IOException e) {
            logger.error("读取历史记录失败: {}", recordId, e);
            return null;
        }
    }
    
    @Override
    public boolean updateRecord(String recordId, OutlineResult outline, HistoryRecord.ImagesInfo images,
                              String status, String thumbnail) {
        HistoryRecord record = getRecord(recordId);
        if (record == null) {
            return false;
        }
        
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        record.setUpdatedAt(now);
        
        if (outline != null) {
            record.setOutline(outline);
        }
        if (images != null) {
            record.setImages(images);
        }
        if (status != null) {
            record.setStatus(status);
        }
        if (thumbnail != null) {
            record.setThumbnail(thumbnail);
        }
        
        saveRecord(record);
        updateIndex(record, "update");
        
        return true;
    }
    
    @Override
    public boolean deleteRecord(String recordId) {
        HistoryRecord record = getRecord(recordId);
        if (record == null) {
            return false;
        }
        
        try {
            // 删除任务目录
            if (record.getImages() != null && record.getImages().getTaskId() != null) {
                Path taskDir = historyDir.resolve(record.getImages().getTaskId());
                if (Files.exists(taskDir)) {
                    Files.walk(taskDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    logger.warn("删除文件失败: {}", path, e);
                                }
                            });
                }
            }
            
            // 删除记录文件
            Path recordFile = historyDir.resolve(recordId + ".json");
            Files.delete(recordFile);
            
            // 从索引中移除
            updateIndex(record, "delete");
            
            logger.info("删除历史记录: {}", recordId);
            return true;
            
        } catch (IOException e) {
            logger.error("删除历史记录失败: {}", recordId, e);
            return false;
        }
    }
    
    @Override
    public PagedResult<HistoryRecord> listRecords(int page, int pageSize, String status) {
        Map<String, Object> index = loadIndex();
        List<Map<String, Object>> records = (List<Map<String, Object>>) index.get("records");
        
        if (records == null) {
            records = new ArrayList<>();
        }
        
        // 状态筛选
        if (status != null && !status.trim().isEmpty()) {
            records = records.stream()
                    .filter(r -> status.equals(r.get("status")))
                    .collect(Collectors.toList());
        }
        
        int total = records.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        
        List<HistoryRecord> pageRecords = new ArrayList<>();
        for (int i = from; i < to; i++) {
            String recordId = (String) records.get(i).get("id");
            HistoryRecord record = getRecord(recordId);
            if (record != null) {
                pageRecords.add(record);
            }
        }
        
        PagedResult<HistoryRecord> result = new PagedResult<>();
        result.setRecords(pageRecords);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotalPages((total + pageSize - 1) / pageSize);
        
        return result;
    }
    
    @Override
    public List<HistoryRecord> searchRecords(String keyword) {
        Map<String, Object> index = loadIndex();
        List<Map<String, Object>> records = (List<Map<String, Object>>) index.get("records");
        
        if (records == null || keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        return records.stream()
                .filter(r -> {
                    String title = (String) r.get("title");
                    return title != null && title.toLowerCase().contains(lowerKeyword);
                })
                .map(r -> {
                    String recordId = (String) r.get("id");
                    return getRecord(recordId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public HistoryStats getStatistics() {
        Map<String, Object> index = loadIndex();
        List<Map<String, Object>> records = (List<Map<String, Object>>) index.get("records");
        
        int total = records != null ? records.size() : 0;
        Map<String, Integer> statusCount = new HashMap<>();
        
        if (records != null) {
            for (Map<String, Object> record : records) {
                String status = (String) record.getOrDefault("status", "draft");
                statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);
            }
        }
        
        HistoryStats stats = new HistoryStats();
        stats.setTotal(total);
        stats.setByStatus(statusCount);
        return stats;
    }
    
    /**
     * 初始化历史目录
     */
    private void initializeHistoryDirectory() {
        try {
            Files.createDirectories(historyDir);
            
            if (!Files.exists(indexFile)) {
                Map<String, Object> initialIndex = new HashMap<>();
                initialIndex.put("records", new ArrayList<>());
                saveIndex(initialIndex);
            }
        } catch (IOException e) {
            logger.error("初始化历史目录失败", e);
        }
    }
    
    /**
     * 加载索引
     */
    private Map<String, Object> loadIndex() {
        if (indexCache != null) {
            return indexCache;
        }
        
        try {
            if (Files.exists(indexFile)) {
                indexCache = objectMapper.readValue(indexFile.toFile(), Map.class);
            } else {
                indexCache = new HashMap<>();
                indexCache.put("records", new ArrayList<>());
            }
        } catch (IOException e) {
            logger.error("加载索引失败", e);
            indexCache = new HashMap<>();
            indexCache.put("records", new ArrayList<>());
        }
        
        return indexCache;
    }
    
    /**
     * 保存索引
     */
    private void saveIndex(Map<String, Object> index) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(indexFile.toFile(), index);
        } catch (IOException e) {
            logger.error("保存索引失败", e);
        }
    }
    
    /**
     * 更新索引
     */
    @SuppressWarnings("unchecked")
    private void updateIndex(HistoryRecord record, String operation) {
        Map<String, Object> index = loadIndex();
        List<Map<String, Object>> records = (List<Map<String, Object>>) index.get("records");

        Map<String, Object> indexRecord = new HashMap<>();
        indexRecord.put("id", record.getId());
        indexRecord.put("title", record.getTitle());
        indexRecord.put("createdAt", record.getCreatedAt());
        indexRecord.put("updatedAt", record.getUpdatedAt());
        indexRecord.put("status", record.getStatus());
        indexRecord.put("thumbnail", record.getThumbnail());
        indexRecord.put("pageCount", record.getOutline() != null && record.getOutline().getPages() != null ?
                record.getOutline().getPages().size() : 0);
        if (record.getImages() != null && record.getImages().getTaskId() != null) {
            indexRecord.put("taskId", record.getImages().getTaskId());
        }

        switch (operation) {
            case "create":
                records.add(0, indexRecord); // 添加到开头
                break;
            case "update":
                records.removeIf(r -> record.getId().equals(r.get("id")));
                records.add(0, indexRecord);
                break;
            case "delete":
                records.removeIf(r -> record.getId().equals(r.get("id")));
                break;
        }
        
        saveIndex(index);
        indexCache = index; // 更新缓存
    }
    
    /**
     * 保存记录
     */
    private void saveRecord(HistoryRecord record) {
        Path recordFile = historyDir.resolve(record.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(recordFile.toFile(), record);
        } catch (IOException e) {
            logger.error("保存记录失败: {}", record.getId(), e);
        }
    }
}