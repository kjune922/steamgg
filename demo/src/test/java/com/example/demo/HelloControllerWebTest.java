package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HelloControllerWebTest {

    private MockMvc mockMvc;
    private GameRepository gameRepository;
    private SteamService steamService;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        steamService = mock(SteamService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HelloController(gameRepository, steamService, "admin-token"))
                .build();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void gamesEndpointReturnsFrontendPageContract() throws Exception {
        when(gameRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(game("elden-ring", "Elden Ring", "RPG", List.of("Open World", "Soulslike"), 4.8, 97))
        ));

        mockMvc.perform(get("/api/games")
                        .param("q", "ring")
                        .param("genre", "RPG")
                        .param("tag", "Open World")
                        .param("sort", "rating")
                        .param("page", "1")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("elden-ring"))
                .andExpect(jsonPath("$.items[0].title").value("Elden Ring"))
                .andExpect(jsonPath("$.items[0].coverImageUrl").value("https://cdn.cloudflare.steamstatic.com/steam/apps/elden-ring/header.jpg"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(12));
    }

    @Test
    void gameDetailReturnsNotFoundForMissingGame() throws Exception {
        mockMvc.perform(get("/api/games/missing-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void remainingGameEndpointsReturnFrontendContracts() throws Exception {
        Games base = game("elden-ring", "Elden Ring", "RPG", List.of("Open World", "Soulslike"), 4.8, 97);
        Games similar = game("cyberpunk", "Cyberpunk 2077", "RPG", List.of("Open World", "Sci-Fi"), 4.5, 89);
        Games other = game("balatro", "Balatro", "Strategy", List.of("Deckbuilder"), 4.7, 92);

        when(gameRepository.findById("elden-ring")).thenReturn(Optional.of(base));
        when(gameRepository.findDistinctGenres()).thenReturn(List.of("RPG"));
        when(gameRepository.findDistinctTags()).thenReturn(List.of("Deckbuilder", "Open World"));
        when(gameRepository.findTop10ByOrderByPopularityDescTitleAsc()).thenReturn(List.of(base, similar, other));
        when(gameRepository.findTop10ByReviewTotalGreaterThanOrderByRatingDescReviewTotalDescTitleAsc(0)).thenReturn(List.of(base, other, similar));
        when(gameRepository.findTop10ByPriceLabelIgnoreCaseOrderByPopularityDescTitleAsc("Free")).thenReturn(List.of());
        when(gameRepository.findSimilarByGenreOrTags(any(), any(), any(), any(Pageable.class))).thenReturn(List.of(similar));

        mockMvc.perform(get("/api/games/facets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres[0]").value("RPG"))
                .andExpect(jsonPath("$.tags[0]").value("Deckbuilder"));

        mockMvc.perform(get("/api/games/curations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("popular"))
                .andExpect(jsonPath("$[0].items[0].id").value("elden-ring"));

        mockMvc.perform(get("/api/games/elden-ring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("elden-ring"))
                .andExpect(jsonPath("$.tags[0]").value("Open World"));

        mockMvc.perform(get("/api/games/elden-ring/similar").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cyberpunk"));
    }

    @Test
    void adminSyncEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/admin/sync"))
                .andExpect(status().isForbidden());

        when(steamService.syncPopularGames()).thenReturn(new SteamService.SyncReport(
                1,
                1,
                0,
                0,
                0,
                50,
                List.of(new SteamService.SyncItemResult("10", "created", "Game 10"))
        ));

        mockMvc.perform(get("/api/admin/sync").header("X-Admin-Token", "admin-token"))
                .andExpect(status().isOk());
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
                "https://cdn.cloudflare.steamstatic.com/steam/apps/" + id + "/header.jpg",
                rating,
                popularity,
                "$19.99",
                title + " short description",
                title + " full description",
                "https://store.steampowered.com/app/" + id
        );
    }
}
