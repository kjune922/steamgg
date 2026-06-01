package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelloControllerTest {

    private final GameRepository gameRepository = mock(GameRepository.class);
    private final SteamService steamService = mock(SteamService.class);
    private final HelloController controller = new HelloController(gameRepository, steamService, "admin-token");

    @Test
    void healthReturnsOk() {
        assertThat(controller.health()).containsEntry("status", "ok");
    }

    @Test
    void adminSyncRequiresToken() {
        assertThat(controller.syncSteam(null, "").getStatusCode().value()).isEqualTo(403);

        when(steamService.syncPopularGames()).thenReturn(new SteamService.SyncReport(
                2,
                2,
                0,
                0,
                0,
                100,
                List.of(
                        new SteamService.SyncItemResult("10", "created", "Game 10"),
                        new SteamService.SyncItemResult("20", "created", "Game 20")
                )
        ));

        assertThat(controller.syncSteam("admin-token", "").getBody())
                .isInstanceOfSatisfying(SteamService.SyncReport.class, report -> {
                    assertThat(report.requested()).isEqualTo(2);
                    assertThat(report.created()).isEqualTo(2);
                });
    }

    @Test
    void gamesEndpointFiltersSortsAndPaginates() {
        when(gameRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                game("elden-ring", "Elden Ring", "RPG", List.of("Open World", "Soulslike"), 4.8, 97)
        )));

        HelloController.GamePageResponse response = controller.getGames(
                "ring",
                "RPG",
                "Open World",
                "rating",
                1,
                12
        );

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.items()).extracting(HelloController.GameDto::id).containsExactly("elden-ring");
    }

    @Test
    void facetsAreCollectedFromGenresAndTags() {
        when(gameRepository.findDistinctGenres()).thenReturn(List.of("RPG"));
        when(gameRepository.findDistinctTags()).thenReturn(List.of("Soulslike", "Open World", "Sci-Fi"));

        HelloController.GameFacetsResponse response = controller.getGameFacets();

        assertThat(response.genres()).containsExactly("RPG");
        assertThat(response.tags()).containsExactly("Open World", "Sci-Fi", "Soulslike");
    }

    @Test
    void curationsDoNotReturnEmptySections() {
        when(gameRepository.findTop10ByOrderByPopularityDescTitleAsc()).thenReturn(List.of());
        when(gameRepository.findTop10ByReviewTotalGreaterThanOrderByRatingDescReviewTotalDescTitleAsc(0)).thenReturn(List.of());
        when(gameRepository.findTop10ByPriceLabelIgnoreCaseOrderByPopularityDescTitleAsc("Free")).thenReturn(List.of());
        assertThat(controller.getGameCurations()).isEmpty();

        when(gameRepository.findTop10ByOrderByPopularityDescTitleAsc()).thenReturn(List.of(
                game("elden-ring", "Elden Ring", "RPG", List.of("Open World"), 0.0, 97)
        ));
        when(gameRepository.findTop10ByReviewTotalGreaterThanOrderByRatingDescReviewTotalDescTitleAsc(0)).thenReturn(List.of(
                game("elden-ring", "Elden Ring", "RPG", List.of("Open World"), 4.8, 97)
        ));

        assertThat(controller.getGameCurations())
                .extracting(HelloController.GameCurationSection::key)
                .containsExactly("popular", "top-rated");
    }

    @Test
    void detailAndSimilarEndpointsReturnExpectedGames() {
        Games base = game("elden-ring", "Elden Ring", "RPG", List.of("Open World", "Soulslike"), 4.8, 97);
        Games similar = game("cyberpunk", "Cyberpunk 2077", "RPG", List.of("Open World", "Sci-Fi"), 4.5, 89);
        Games other = game("balatro", "Balatro", "Strategy", List.of("Card Game"), 4.7, 92);

        when(gameRepository.findById("elden-ring")).thenReturn(Optional.of(base));
        when(gameRepository.findSimilarByGenreOrTags(
                eq("elden-ring"),
                eq("RPG"),
                eq(List.of("open world", "soulslike")),
                any(Pageable.class)
        )).thenReturn(List.of(similar));

        assertThat(controller.getGame("elden-ring").getBody().id()).isEqualTo("elden-ring");
        assertThat(controller.getSimilarGames("elden-ring", 3))
                .extracting(HelloController.GameDto::id)
                .containsExactly("cyberpunk");
    }

    private Games game(
            String id,
            String title,
            String genre,
            List<String> tags,
            double rating,
            int popularity
    ) {
        return new Games(
                id,
                title,
                genre,
                tags,
                "https://cdn.example.com/" + id + ".jpg",
                rating,
                popularity,
                "$19.99",
                title + " short description",
                title + " full description",
                "https://store.steampowered.com/app/" + id
        );
    }
}
