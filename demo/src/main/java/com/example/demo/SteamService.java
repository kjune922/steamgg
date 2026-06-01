package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SteamService {

    private static final double UNKNOWN_RATING = 0.0;
    private static final double REVIEW_PRIOR_POSITIVE_RATIO = 0.75;
    private static final int REVIEW_PRIOR_WEIGHT = 100;

    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean autoSyncEnabled;

    public SteamService(
            GameRepository gameRepository,
            RestTemplate restTemplate,
            @Value("${app.steam.auto-sync-enabled:false}") boolean autoSyncEnabled
    ) {
        this.gameRepository = gameRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.autoSyncEnabled = autoSyncEnabled;
    }

    @Scheduled(fixedDelay = 3600000)
    public void autoSyncSteamGames() {
        if (!autoSyncEnabled) {
            return;
        }

        SyncReport report = syncPopularGames();
        System.out.println(">>> [스케줄러] 정기 수집 완료: " + report.created() + " created, "
                + report.updated() + " updated, " + report.skipped() + " skipped, "
                + report.failed() + " failed");
    }

    public SyncReport syncPopularGames() {
        Instant startedAt = Instant.now();
        List<String> targetAppIds = fetchPopularAppIds();
        List<SyncItemResult> results = new ArrayList<>();

        for (int index = 0; index < targetAppIds.size(); index++) {
            String appId = targetAppIds.get(index);
            int popularity = Math.max(1, 100 - index);
            results.add(fetchAndSaveGame(appId, popularity));
            pauseBetweenSteamRequests();
        }

        int created = countStatus(results, "created");
        int updated = countStatus(results, "updated");
        int skipped = countStatus(results, "skipped");
        int failed = countStatus(results, "failed");

        return new SyncReport(
                targetAppIds.size(),
                created,
                updated,
                skipped,
                failed,
                Duration.between(startedAt, Instant.now()).toMillis(),
                results
        );
    }

    public SyncItemResult fetchAndSaveGame(String appId) {
        return fetchAndSaveGame(appId, 0);
    }

    public SyncItemResult fetchAndSaveGame(String appId, int popularity) {
        try {
            Optional<Games> existingGame = gameRepository.findById(appId);
            if (existingGame.isPresent()) {
                Games game = existingGame.get();
                ReviewSummary reviewSummary = fetchReviewSummary(appId);
                game.updatePopularity(popularity);
                if (reviewSummary.available()) {
                    game.updateReviewMetrics(
                            reviewSummary.rating(),
                            reviewSummary.positive(),
                            reviewSummary.negative(),
                            reviewSummary.total(),
                            reviewSummary.scoreDescription()
                    );
                }
                gameRepository.save(game);
                return new SyncItemResult(appId, "updated", syncMessage(game.getTitle(), reviewSummary));
            }

            String url = "https://store.steampowered.com/api/appdetails?appids=" + appId + "&l=korean";
            String jsonString = restTemplate.getForObject(url, String.class);
            JsonNode response = objectMapper.readTree(jsonString);
            if (response == null || !response.has(appId)) {
                return new SyncItemResult(appId, "failed", "missing Steam response");
            }

            JsonNode root = response.get(appId);
            if (!root.path("success").asBoolean(false)) {
                return new SyncItemResult(appId, "skipped", "Steam returned success=false");
            }

            JsonNode data = root.path("data");
            if (!"game".equals(data.path("type").asText())) {
                return new SyncItemResult(appId, "skipped", "Steam app is not a game");
            }

            String title = data.path("name").asText("Untitled");
            String genre = firstDescription(data.path("genres"), "Unknown");
            String coverUrl = data.path("header_image").asText("");
            String shortDesc = stripHtml(data.path("short_description").asText(""));
            String description = stripHtml(data.path("detailed_description").asText(""));
            if (description.isBlank()) {
                description = shortDesc;
            }
            String price = data.hasNonNull("price_overview")
                    ? data.path("price_overview").path("final_formatted").asText("Free")
                    : "Free";

            List<String> tags = new ArrayList<>();
            JsonNode categories = data.path("categories");
            if (categories.isArray()) {
                categories.forEach(category -> {
                    String categoryDescription = category.path("description").asText("");
                    if (!categoryDescription.isBlank()) {
                        tags.add(categoryDescription);
                    }
                });
            }

            ReviewSummary reviewSummary = fetchReviewSummary(appId);
            Games game = new Games(
                    appId,
                    title,
                    genre,
                    tags,
                    coverUrl,
                    reviewSummary.rating(),
                    popularity,
                    price,
                    shortDesc,
                    description,
                    "https://store.steampowered.com/app/" + appId,
                    reviewSummary.positive(),
                    reviewSummary.negative(),
                    reviewSummary.total(),
                    reviewSummary.scoreDescription()
            );

            gameRepository.save(game);
            return new SyncItemResult(appId, "created", syncMessage(title, reviewSummary));
        } catch (Exception e) {
            return new SyncItemResult(appId, "failed", e.getMessage());
        }
    }

    public List<String> fetchPopularAppIds() {
        String url = "https://store.steampowered.com/api/featuredcategories";
        List<String> popularIds = new ArrayList<>();

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonString);

            JsonNode newReleases = root.path("new_releases").path("items");
            if (newReleases.isArray()) {
                for (JsonNode item : newReleases) {
                    String id = item.path("id").asText("");
                    if (!id.isBlank()) {
                        popularIds.add(id);
                    }
                }
            }

            System.out.println("자동으로 찾은 게임 개수: " + popularIds.size());
        } catch (Exception e) {
            System.out.println("ID 목록 가져오기 실패: " + e.getMessage());
        }

        return popularIds;
    }

    private ReviewSummary fetchReviewSummary(String appId) {
        String url = "https://store.steampowered.com/appreviews/" + appId
                + "?json=1&filter=all&language=all&review_type=all&purchase_type=all&num_per_page=1";

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonString);
            if (root == null || root.path("success").asInt(0) != 1) {
                return ReviewSummary.unavailable();
            }

            JsonNode summary = root.path("query_summary");
            int positive = Math.max(0, summary.path("total_positive").asInt(0));
            int negative = Math.max(0, summary.path("total_negative").asInt(0));
            int total = Math.max(summary.path("total_reviews").asInt(0), positive + negative);
            String scoreDescription = summary.path("review_score_desc").asText("");

            return new ReviewSummary(
                    positive,
                    negative,
                    total,
                    scoreDescription,
                    calculateAdjustedRating(positive, total),
                    true
            );
        } catch (Exception e) {
            return ReviewSummary.unavailable();
        }
    }

    private double calculateAdjustedRating(int positive, int total) {
        if (total <= 0) {
            return UNKNOWN_RATING;
        }

        int safePositive = Math.max(0, Math.min(positive, total));
        double adjustedPositiveRatio = (safePositive + REVIEW_PRIOR_WEIGHT * REVIEW_PRIOR_POSITIVE_RATIO)
                / (total + (double) REVIEW_PRIOR_WEIGHT);
        return Math.round(adjustedPositiveRatio * 5.0 * 10.0) / 10.0;
    }

    private String syncMessage(String title, ReviewSummary reviewSummary) {
        if (!reviewSummary.available()) {
            return title + " (reviews unavailable)";
        }
        if (reviewSummary.total() == 0) {
            return title + " (no Steam reviews)";
        }
        return title + " (" + reviewSummary.positive() + "/" + reviewSummary.total() + " positive reviews)";
    }

    private int countStatus(List<SyncItemResult> results, String status) {
        return (int) results.stream()
                .filter(result -> status.equals(result.status()))
                .count();
    }

    private void pauseBetweenSteamRequests() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String firstDescription(JsonNode values, String fallback) {
        if (values.isArray() && !values.isEmpty()) {
            String description = values.get(0).path("description").asText("");
            if (!description.isBlank()) {
                return description;
            }
        }
        return fallback;
    }

    private String stripHtml(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "").trim();
    }

    public record SyncReport(
            int requested,
            int created,
            int updated,
            int skipped,
            int failed,
            long durationMillis,
            List<SyncItemResult> items
    ) {
    }

    public record SyncItemResult(String appId, String status, String message) {
    }

    private record ReviewSummary(
            int positive,
            int negative,
            int total,
            String scoreDescription,
            double rating,
            boolean available
    ) {
        static ReviewSummary unavailable() {
            return new ReviewSummary(0, 0, 0, "", UNKNOWN_RATING, false);
        }
    }
}
