package com.redink.model;

import lombok.Data;

/**
 * 页面数据模型
 */
@Data
public class Page {
    private int index;
    private String type; // cover, content, summary
    private String content;
    
    public Page() {}
    
    public Page(int index, String type, String content) {
        this.index = index;
        this.type = type;
        this.content = content;
    }
}