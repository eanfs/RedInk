package com.redink.service;

import lombok.Data;
import java.util.Map;

/**
 * 历史统计信息
 */
@Data
public class HistoryStats {
    private int total;
    private Map<String, Integer> byStatus;
}