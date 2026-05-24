package com.chriswininger.documentmcp.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int totalCount,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {}
