package com.example.demo;

import java.util.List;

public record GamePageResponse(
        List<Games> items,
        long totalCount,
        int totalPages,
        int page,
        int size
) {
}
