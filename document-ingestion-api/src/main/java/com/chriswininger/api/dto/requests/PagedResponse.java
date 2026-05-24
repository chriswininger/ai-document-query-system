package com.chriswininger.api.dto.requests;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        long totalCount,
        int currentPage,
        int pageSize,
        int totalPages,
        boolean hasNextPage,
        boolean hasPreviousPage
) {}
