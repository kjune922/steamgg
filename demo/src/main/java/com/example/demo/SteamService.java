package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SteamService {
    private static final double DEFAULT_RATING = 0.0;
    private static final int DEFAULT_POPULARITY = 0;
    private static final String DEFAULT_GENRE = "Unknown";
    private static final String DEFAULT_PRICE = "Free";
    private static final int MAX_SYNC_APP_IDS = 30;
    private static final int MAX_POPULARITY_SCORE = 100;
    private static final int POPULARITY_LOG_SCALE_MAX = 7;

    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${steam.sync.enabled:true}")
    private boolean steamSyncEnabled;

    public SteamService(GameRepository gameRepository, RestTemplate restTemplate) {
        this.gameRepository = gameRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedDelay = 3600000)
    public void autoSyncSteamGames() {
        if (!steamSyncEnabled) {
            return;
        }

        System.out.println(">>> [scheduler] Steam sync started");
        int count = syncPopularGames();
        System.out.println(">>> [scheduler] Steam sync finished: " + count + " games");
    }

    public int syncPopularGames() {
        if (!steamSyncEnabled) {
            return 0;
        }

        List<String> popularIds = fetchPopularAppIds();
        Map<String, Integer> chartPopularityScores = fetchMostPlayedPopularityScores();
        int savedCount = 0;
        for (String appId : popularIds) {
            int chartPopularityScore = chartPopularityScores.getOrDefault(appId, DEFAULT_POPULARITY);
            if (fetchAndSaveGame(appId, chartPopularityScore)) {
                savedCount++;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return savedCount;
            }
        }

        return savedCount;
    }

    public boolean fetchAndSaveGame(String appId) {
        return fetchAndSaveGame(appId, DEFAULT_POPULARITY);
    }

    public boolean fetchAndSaveGame(String appId, int chartPopularityScore) {
        if (gameRepository.existsById(appId)) {
            return false;
        }

        String url = "https://store.steampowered.com/api/appdetails?appids=" + appId + "&l=korean";

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            if (jsonString == null || jsonString.isBlank()) {
                return false;
            }

            JsonNode response = objectMapper.readTree(jsonString);
            JsonNode root = response.path(appId);
            if (root.isMissingNode() || !root.path("success").asBoolean(false)) {
                return false;
            }

            JsonNode data = root.path("data");
            if (data.isMissingNode() || !"game".equals(data.path("type").asText())) {
                return false;
            }

            SteamReviewSummary reviewSummary = fetchReviewSummary(appId);
            Games game = mapSteamGame(appId, data, reviewSummary, chartPopularityScore);
            gameRepository.save(game);
            System.out.println("Steam game saved: " + game.getTitle());
            return true;
        } catch (Exception e) {
            System.out.println("Steam game sync failed (ID: " + appId + "): " + e.getMessage());
            return false;
        }
    }

    public List<String> fetchPopularAppIds() {
        String url = "https://store.steampowered.com/api/featuredcategories";
        Set<String> popularIds = new LinkedHashSet<>();

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            if (jsonString == null || jsonString.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(jsonString);
            addCategoryAppIds(popularIds, root.path("top_sellers").path("items"));
            addCategoryAppIds(popularIds, root.path("new_releases").path("items"));
            addCategoryAppIds(popularIds, root.path("specials").path("items"));
        } catch (Exception e) {
            System.out.println("Steam popular app ID fetch failed: " + e.getMessage());
        }

        return popularIds.stream().limit(MAX_SYNC_APP_IDS).toList();
    }

    public Map<String, Integer> fetchMostPlayedPopularityScores() {
        String url = "https://api.steampowered.com/ISteamChartsService/GetMostPlayedGames/v1/?format=json";

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            if (jsonString == null || jsonString.isBlank()) {
                return Map.of();
            }

            JsonNode ranks = objectMapper.readTree(jsonString).path("response").path("ranks");
            if (!ranks.isArray()) {
                return Map.of();
            }

            Map<String, Integer> scores = new HashMap<>();
            for (JsonNode rankNode : ranks) {
                String appId = rankNode.path("appid").asText("");
                int rank = rankNode.path("rank").asInt(0);
                if (!appId.isBlank() && rank > 0) {
                    scores.put(appId, Math.max(1, MAX_POPULARITY_SCORE + 1 - rank));
                }
            }
            return scores;
        } catch (Exception e) {
            System.out.println("Steam most played fetch failed: " + e.getMessage());
            return Map.of();
        }
    }

    SteamReviewSummary fetchReviewSummary(String appId) {
        String url = "https://store.steampowered.com/appreviews/" + appId
                + "?json=1&filter=summary&language=all&purchase_type=steam&num_per_page=0";

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            if (jsonString == null || jsonString.isBlank()) {
                return SteamReviewSummary.empty();
            }

            JsonNode summary = objectMapper.readTree(jsonString).path("query_summary");
            if (summary.isMissingNode()) {
                return SteamReviewSummary.empty();
            }

            return new SteamReviewSummary(
                    summary.path("total_positive").asInt(0),
                    summary.path("total_negative").asInt(0),
                    summary.path("total_reviews").asInt(0),
                    summary.path("review_score_desc").asText("")
            );
        } catch (Exception e) {
            System.out.println("Steam review summary fetch failed (ID: " + appId + "): " + e.getMessage());
            return SteamReviewSummary.empty();
        }
    }

    Games mapSteamGame(String appId, JsonNode data) {
        return mapSteamGame(appId, data, SteamReviewSummary.empty(), DEFAULT_POPULARITY);
    }

    Games mapSteamGame(
            String appId,
            JsonNode data,
            SteamReviewSummary reviewSummary,
            int chartPopularityScore
    ) {
        String title = text(data, "name", appId);
        String genre = firstDescription(data.path("genres"), DEFAULT_GENRE);
        String coverUrl = text(data, "header_image", "");
        String shortDescription = stripHtml(text(data, "short_description", ""));
        String description = stripHtml(text(data, "about_the_game", shortDescription));
        if (description.isBlank()) {
            description = shortDescription;
        }
        String price = data.hasNonNull("price_overview")
                ? text(data.path("price_overview"), "final_formatted", DEFAULT_PRICE)
                : DEFAULT_PRICE;
        List<String> tags = descriptions(data.path("categories"));
        if (tags.isEmpty() && !DEFAULT_GENRE.equals(genre)) {
            tags.add(genre);
        }
        int recommendationCount = data.path("recommendations").path("total").asInt(reviewSummary.totalReviews());

        return new Games(
                appId,
                title,
                genre,
                tags,
                coverUrl,
                ratingFromReviews(reviewSummary),
                popularityScore(recommendationCount, chartPopularityScore),
                price,
                shortDescription,
                description,
                "https://store.steampowered.com/app/" + appId
        );
    }

    private void addCategoryAppIds(Set<String> appIds, JsonNode items) {
        if (!items.isArray()) {
            return;
        }

        for (JsonNode item : items) {
            String id = item.path("id").asText("");
            if (!id.isBlank()) {
                appIds.add(id);
            }
        }
    }

    private double ratingFromReviews(SteamReviewSummary reviewSummary) {
        if (reviewSummary.totalReviews() <= 0) {
            return DEFAULT_RATING;
        }

        double rating = ((double) reviewSummary.totalPositive() / reviewSummary.totalReviews()) * 5.0;
        return Math.round(rating * 10.0) / 10.0;
    }

    private int popularityScore(int recommendationCount, int chartPopularityScore) {
        int recommendationScore = popularityScoreFromCount(recommendationCount);
        int safeChartScore = Math.max(DEFAULT_POPULARITY, Math.min(chartPopularityScore, MAX_POPULARITY_SCORE));
        return Math.max(recommendationScore, safeChartScore);
    }

    private int popularityScoreFromCount(int count) {
        if (count <= 0) {
            return DEFAULT_POPULARITY;
        }

        double score = Math.log10(count + 1.0) / POPULARITY_LOG_SCALE_MAX * MAX_POPULARITY_SCORE;
        return Math.max(1, Math.min(MAX_POPULARITY_SCORE, (int) Math.round(score)));
    }

    private String text(JsonNode node, String fieldName, String defaultValue) {
        String value = node.path(fieldName).asText(defaultValue);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String firstDescription(JsonNode nodes, String defaultValue) {
        if (!nodes.isArray() || nodes.isEmpty()) {
            return defaultValue;
        }
        return text(nodes.get(0), "description", defaultValue);
    }

    private List<String> descriptions(JsonNode nodes) {
        List<String> values = new ArrayList<>();
        if (!nodes.isArray()) {
            return values;
        }

        for (JsonNode node : nodes) {
            String description = node.path("description").asText("");
            if (!description.isBlank()) {
                values.add(description);
            }
        }
        return values;
    }

    private String stripHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("<[^>]*>", "").trim();
    }

    record SteamReviewSummary(
            int totalPositive,
            int totalNegative,
            int totalReviews,
            String scoreDescription
    ) {
        static SteamReviewSummary empty() {
            return new SteamReviewSummary(0, 0, 0, "");
        }
    }
}
