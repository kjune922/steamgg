package com.example.demo;

import java.util.List;

public record GameFacets(
        List<String> genres,
        List<String> tags
) {
}
