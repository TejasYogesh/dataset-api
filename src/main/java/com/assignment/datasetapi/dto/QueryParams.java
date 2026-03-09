package com.assignment.datasetapi.dto;

import lombok.Data;

/**
 * Query URL examples:
 * GET /api/dataset/employee_dataset/query?groupBy=department
 * GET /api/dataset/employee_dataset/query?sortBy=age&order=asc
 * GET /api/dataset/employee_dataset/query?sortBy=name&order=desc
 */
@Data
public class QueryParams {
    
    private String groupBy;
    private String sortBy;
    private String order = "asc";
    public boolean hasGroupBy() {
        return groupBy != null && !groupBy.isBlank();
    }
    public boolean hasSortBy() {
        return sortBy != null && !sortBy.isBlank();
    }
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(order);
    }
}
