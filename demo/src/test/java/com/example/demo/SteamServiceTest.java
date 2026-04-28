package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SteamServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapSteamGameUsesDefaultsWhenOptionalFieldsAreMissing() throws Exception {
        SteamService steamService = new SteamService(mock(GameRepository.class), new RestTemplate());
        JsonNode data = objectMapper.readTree("""
                {
                  "type": "game",
                  "name": "Minimal Game"
                }
                """);

        Games game = steamService.mapSteamGame("123", data);

        assertThat(game.getId()).isEqualTo("123");
        assertThat(game.getTitle()).isEqualTo("Minimal Game");
        assertThat(game.getGenre()).isEqualTo("Unknown");
        assertThat(game.getTags()).isEmpty();
        assertThat(game.getPriceLabel()).isEqualTo("Free");
        assertThat(game.getRating()).isEqualTo(0.0);
        assertThat(game.getPopularity()).isEqualTo(0);
        assertThat(game.getPurchaseUrl()).isEqualTo("https://store.steampowered.com/app/123");
    }

    @Test
    void mapSteamGameStripsHtmlAndReadsCollections() throws Exception {
        SteamService steamService = new SteamService(mock(GameRepository.class), new RestTemplate());
        JsonNode data = objectMapper.readTree("""
                {
                  "type": "game",
                  "name": "Full Game",
                  "genres": [{ "description": "Adventure" }],
                  "categories": [{ "description": "Single-player" }],
                  "short_description": "<b>Short</b> text",
                  "about_the_game": "<p>Long</p> text",
                  "header_image": "https://example.com/header.jpg",
                  "price_overview": { "final_formatted": "$9.99" }
                }
                """);

        Games game = steamService.mapSteamGame("456", data);

        assertThat(game.getGenre()).isEqualTo("Adventure");
        assertThat(game.getTags()).containsExactly("Single-player");
        assertThat(game.getShortDescription()).isEqualTo("Short text");
        assertThat(game.getDescription()).isEqualTo("Long text");
        assertThat(game.getCoverImageUrl()).isEqualTo("https://example.com/header.jpg");
        assertThat(game.getPriceLabel()).isEqualTo("$9.99");
    }

    @Test
    void mapSteamGameCalculatesRatingAndPopularityFromSteamSignals() throws Exception {
        SteamService steamService = new SteamService(mock(GameRepository.class), new RestTemplate());
        JsonNode data = objectMapper.readTree("""
                {
                  "type": "game",
                  "name": "Reviewed Game",
                  "recommendations": { "total": 10000 }
                }
                """);

        Games game = steamService.mapSteamGame(
                "789",
                data,
                new SteamService.SteamReviewSummary(80, 20, 100, "Very Positive"),
                0
        );

        assertThat(game.getRating()).isEqualTo(4.0);
        assertThat(game.getPopularity()).isEqualTo(57);
    }

    @Test
    void mapSteamGameUsesChartPopularityWhenItIsStrongerThanRecommendationCount() throws Exception {
        SteamService steamService = new SteamService(mock(GameRepository.class), new RestTemplate());
        JsonNode data = objectMapper.readTree("""
                {
                  "type": "game",
                  "name": "Chart Game",
                  "recommendations": { "total": 10 }
                }
                """);

        Games game = steamService.mapSteamGame(
                "999",
                data,
                new SteamService.SteamReviewSummary(1, 1, 2, "Mixed"),
                82
        );

        assertThat(game.getRating()).isEqualTo(2.5);
        assertThat(game.getPopularity()).isEqualTo(82);
    }
}
