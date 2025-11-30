package com.redink.service;

import lombok.Data;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PagedResult<T> {
    private List<T> records;
    private int total;
    private int page;
    private int pageSize;
    private int totalPages;
}