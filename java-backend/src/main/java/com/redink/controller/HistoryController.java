package com.redink.controller;

import com.redink.model.HistoryRecord;
import com.redink.model.OutlineResult;
import com.redink.model.ApiResponse;
import com.redink.service.HistoryService;
import com.redink.service.HistoryStats;
import com.redink.service.PagedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 历史记录API控制器
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);
    
    private final HistoryService historyService;
    
    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }
    
    /**
     * 创建历史记录
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createHistory(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            @SuppressWarnings("unchecked")
            Map<String, Object> outlineMap = (Map<String, Object>) request.get("outline");
            String taskId = (String) request.get("taskId");
            
            // 转换OutlineResult
            OutlineResult outline = new OutlineResult();
            Object successObj = outlineMap.get("success");
            outline.setSuccess(successObj != null ? (Boolean) successObj : false);
            outline.setOutline((String) outlineMap.get("outline"));
            Object hasImagesObj = outlineMap.get("hasImages");
            outline.setHasImages(hasImagesObj != null ? (Boolean) hasImagesObj : false);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pagesMap = (List<Map<String, Object>>) outlineMap.get("pages");
            List<com.redink.model.Page> pages = pagesMap != null ? pagesMap.stream()
                    .map(pageMap -> {
                        com.redink.model.Page page = new com.redink.model.Page();
                        Object indexObj = pageMap.get("index");
                        page.setIndex(indexObj != null ? (Integer) indexObj : 0);
                        page.setType((String) pageMap.get("type"));
                        page.setContent((String) pageMap.get("content"));
                        return page;
                    })
                    .toList() : java.util.Collections.emptyList();
            outline.setPages(pages);
            
            String recordId = historyService.createRecord(topic, outline, taskId);
            
            return ResponseEntity.ok(ApiResponse.success(recordId));
            
        } catch (Exception e) {
            logger.error("创建历史记录异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取历史记录列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResult<HistoryRecord>>> listHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        
        try {
            PagedResult<HistoryRecord> result = historyService.listRecords(page, pageSize, status);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("获取历史记录列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取历史记录详情
     */
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<HistoryRecord>> getHistory(@PathVariable String recordId) {
        try {
            HistoryRecord record = historyService.getRecord(recordId);
            
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("历史记录不存在: " + recordId));
            }
            
            return ResponseEntity.ok(ApiResponse.success(record));
            
        } catch (Exception e) {
            logger.error("获取历史记录详情异常: {}", recordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新历史记录
     */
    @PutMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> updateHistory(@PathVariable String recordId,
                                                         @RequestBody Map<String, Object> request) {
        try {
            OutlineResult outline = null;
            HistoryRecord.ImagesInfo images = null;
            String status = (String) request.get("status");
            String thumbnail = (String) request.get("thumbnail");
            
            if (request.containsKey("outline")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> outlineMap = (Map<String, Object>) request.get("outline");
                outline = new OutlineResult();
                outline.setSuccess((Boolean) outlineMap.get("success"));
                outline.setOutline((String) outlineMap.get("outline"));
                outline.setHasImages((Boolean) outlineMap.get("hasImages"));
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pagesMap = (List<Map<String, Object>>) outlineMap.get("pages");
                List<com.redink.model.Page> pages = pagesMap.stream()
                        .map(pageMap -> {
                            com.redink.model.Page page = new com.redink.model.Page();
                            page.setIndex((Integer) pageMap.get("index"));
                            page.setType((String) pageMap.get("type"));
                            page.setContent((String) pageMap.get("content"));
                            return page;
                        })
                        .toList();
                outline.setPages(pages);
            }
            
            if (request.containsKey("images")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> imagesMap = (Map<String, Object>) request.get("images");
                images = new HistoryRecord.ImagesInfo();
                images.setTaskId((String) imagesMap.get("taskId"));
                
                @SuppressWarnings("unchecked")
                List<String> generatedList = (List<String>) imagesMap.get("generated");
                images.setGenerated(generatedList.toArray(new String[0]));
            }
            
            boolean success = historyService.updateRecord(recordId, outline, images, status, thumbnail);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("更新失败: 记录不存在"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(null, "更新成功"));
            
        } catch (Exception e) {
            logger.error("更新历史记录异常: {}", recordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除历史记录
     */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(@PathVariable String recordId) {
        try {
            boolean success = historyService.deleteRecord(recordId);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("删除失败: 记录不存在"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
            
        } catch (Exception e) {
            logger.error("删除历史记录异常: {}", recordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 搜索历史记录
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<HistoryRecord>>> searchHistory(@RequestParam String keyword) {
        try {
            List<HistoryRecord> results = historyService.searchRecords(keyword);
            return ResponseEntity.ok(ApiResponse.success(results));
            
        } catch (Exception e) {
            logger.error("搜索历史记录异常: keyword={}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<HistoryStats>> getHistoryStats() {
        try {
            HistoryStats stats = historyService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            logger.error("获取历史记录统计异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 下载历史记录ZIP
     */
    @GetMapping("/{recordId}/download")
    public ResponseEntity<byte[]> downloadHistoryZip(@PathVariable String recordId) {
        try {
            HistoryRecord record = historyService.getRecord(recordId);
            
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            String taskId = record.getImages() != null ? record.getImages().getTaskId() : null;
            if (taskId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            Path taskDir = Paths.get("history", taskId);
            if (!Files.exists(taskDir)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // 创建内存中的ZIP文件
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                Files.list(taskDir)
                        .filter(path -> {
                            String filename = path.getFileName().toString();
                            return !filename.startsWith("thumb_") && 
                                   (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg"));
                        })
                        .forEach(path -> {
                            try {
                                String filename = path.getFileName().toString();
                                String archiveName;
                                try {
                                    int index = Integer.parseInt(filename.split("\\.")[0]);
                                    archiveName = "page_" + (index + 1) + ".png";
                                } catch (NumberFormatException e) {
                                    archiveName = filename;
                                }
                                
                                ZipEntry entry = new ZipEntry(archiveName);
                                zos.putNextEntry(entry);
                                
                                byte[] fileBytes = Files.readAllBytes(path);
                                zos.write(fileBytes);
                                zos.closeEntry();
                                
                            } catch (IOException e) {
                                logger.warn("添加文件到ZIP失败: {}", path, e);
                            }
                        });
            }
            
            byte[] zipBytes = baos.toByteArray();
            
            String filename = record.getTitle() != null ? 
                record.getTitle().replaceAll("[^a-zA-Z0-9\\s\\-_]", "").trim() + ".zip" : 
                "images.zip";
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipBytes);
                    
        } catch (Exception e) {
            logger.error("下载历史记录ZIP异常: {}", recordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}