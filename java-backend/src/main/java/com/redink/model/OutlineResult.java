package com.redink.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 大纲生成结果
 */
@Data
@Accessors(chain = true)
public class OutlineResult {
    private boolean success;
    private String outline;
    private List<Page> pages;
    private boolean hasImages;
    private String error;
    
    public static OutlineResultBuilder builder() {
        return new OutlineResultBuilder();
    }
    
    public static class OutlineResultBuilder {
        private boolean success;
        private String outline;
        private List<Page> pages;
        private boolean hasImages;
        private String error;
        
        public OutlineResultBuilder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public OutlineResultBuilder outline(String outline) {
            this.outline = outline;
            return this;
        }
        
        public OutlineResultBuilder pages(List<Page> pages) {
            this.pages = pages;
            return this;
        }
        
        public OutlineResultBuilder hasImages(boolean hasImages) {
            this.hasImages = hasImages;
            return this;
        }
        
        public OutlineResultBuilder error(String error) {
            this.error = error;
            return this;
        }
        
        public OutlineResult build() {
            OutlineResult result = new OutlineResult();
            result.setSuccess(success);
            result.setOutline(outline);
            result.setPages(pages);
            result.setHasImages(hasImages);
            result.setError(error);
            return result;
        }
    }
}