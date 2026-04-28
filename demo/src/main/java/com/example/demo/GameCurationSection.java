package com.example.demo;

import java.util.List;

public record GameCurationSection(
        String key,
        String title,
        String href,
        List<Games> items
) {
}
