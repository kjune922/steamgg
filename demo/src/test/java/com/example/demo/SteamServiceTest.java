package com.example.demo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SteamServiceTest {

    private final GameRepository gameRepository = mock(GameRepository.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final SteamService steamService = new SteamService(gameRepository, restTemplate, false);

    @Test
    void fetchAndSaveGameStoresSteamReviewSummary() {
        when(gameRepository.findById("10")).thenReturn(Optional.empty());
        when(restTemplate.getForObject(contains("/api/appdetails?appids=10"), eq(String.class)))
                .thenReturn("""
                        {
                          "10": {
                            "success": true,
                            "data": {
                              "type": "game",
                              "name": "Portal",
                              "genres": [{"description": "Action"}],
                              "header_image": "https://cdn.example.com/portal.jpg",
                              "short_description": "Short description",
                              "detailed_description": "<p>Detailed description</p>",
                              "price_overview": {"final_formatted": "$9.99"},
                              "categories": [{"description": "Single-player"}]
                            }
                          }
                        }
                        """);
        when(restTemplate.getForObject(contains("/appreviews/10?"), eq(String.class)))
                .thenReturn("""
                        {
                          "success": 1,
                          "query_summary": {
                            "review_score_desc": "Very Positive",
                            "total_positive": 900,
                            "total_negative": 100,
                            "total_reviews": 1000
                          },
                          "reviews": []
                        }
                        """);

        SteamService.SyncItemResult result = steamService.fetchAndSaveGame("10", 99);

        ArgumentCaptor<Games> savedGame = ArgumentCaptor.forClass(Games.class);
        verify(gameRepository).save(savedGame.capture());

        assertThat(result.status()).isEqualTo("created");
        assertThat(savedGame.getValue().getRating()).isEqualTo(4.4);
        assertThat(savedGame.getValue().getReviewPositive()).isEqualTo(900);
        assertThat(savedGame.getValue().getReviewNegative()).isEqualTo(100);
        assertThat(savedGame.getValue().getReviewTotal()).isEqualTo(1000);
        assertThat(savedGame.getValue().getReviewScoreDescription()).isEqualTo("Very Positive");
    }

    @Test
    void fetchAndSaveGameRefreshesReviewSummaryForExistingGame() {
        Games existing = new Games(
                "20",
                "Half-Life 2",
                "Action",
                List.of("Classic"),
                "https://cdn.example.com/hl2.jpg",
                0.0,
                12,
                "$9.99",
                "Short",
                "Long",
                "https://store.steampowered.com/app/20"
        );
        when(gameRepository.findById("20")).thenReturn(Optional.of(existing));
        when(restTemplate.getForObject(contains("/appreviews/20?"), eq(String.class)))
                .thenReturn("""
                        {
                          "success": 1,
                          "query_summary": {
                            "review_score_desc": "Positive",
                            "total_positive": 8,
                            "total_negative": 2,
                            "total_reviews": 10
                          },
                          "reviews": []
                        }
                        """);

        SteamService.SyncItemResult result = steamService.fetchAndSaveGame("20", 98);

        verify(gameRepository).save(existing);
        assertThat(result.status()).isEqualTo("updated");
        assertThat(existing.getPopularity()).isEqualTo(98);
        assertThat(existing.getRating()).isEqualTo(3.8);
        assertThat(existing.getReviewTotal()).isEqualTo(10);
        assertThat(existing.getReviewScoreDescription()).isEqualTo("Positive");
    }
}
