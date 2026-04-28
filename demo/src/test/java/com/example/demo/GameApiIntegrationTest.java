package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class GameApiIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        gameRepository.deleteAll();
        gameRepository.saveAll(List.of(
                new Games(
                        "cozy-farm",
                        "Cozy Farm",
                        "캐주얼",
                        List.of("Cozy", "Farming"),
                        "https://example.com/cozy.jpg",
                        4.8,
                        90,
                        "₩ 10,000",
                        "Casual farming game",
                        "Relaxing farming and crafting.",
                        "https://store.steampowered.com/app/1"
                ),
                new Games(
                        "action-quest",
                        "Action Quest",
                        "액션",
                        List.of("Single-player", "Adventure"),
                        "https://example.com/action.jpg",
                        4.1,
                        80,
                        "₩ 20,000",
                        "Fast action adventure",
                        "Fight through action stages.",
                        "https://store.steampowered.com/app/2"
                ),
                new Games(
                        "strategy-cards",
                        "Strategy Cards",
                        "전략",
                        List.of("Card Game", "Single-player"),
                        "https://example.com/cards.jpg",
                        4.7,
                        70,
                        "₩ 15,000",
                        "Card strategy game",
                        "Build a deck and plan turns.",
                        "https://store.steampowered.com/app/3"
                ),
                new Games(
                        "rpg-legend",
                        "RPG Legend",
                        "RPG",
                        List.of("Fantasy", "Adventure"),
                        "https://example.com/rpg.jpg",
                        4.9,
                        95,
                        "₩ 30,000",
                        "Fantasy RPG",
                        "Explore a fantasy world.",
                        "https://store.steampowered.com/app/4"
                ),
                new Games(
                        "indie-puzzle",
                        "Indie Puzzle",
                        "인디",
                        List.of("Puzzle", "Single-player"),
                        "https://example.com/puzzle.jpg",
                        4.2,
                        60,
                        "Free",
                        "Indie puzzle game",
                        "Solve compact puzzles.",
                        "https://store.steampowered.com/app/5"
                )
        ));
    }

    @Test
    void gamesApiReturnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/games").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value("rpg-legend"));
    }

    @Test
    void gamesApiSearchesAndFilters() throws Exception {
        mockMvc.perform(get("/api/games").param("q", "casual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value("cozy-farm"));

        mockMvc.perform(get("/api/games").param("genre", "액션").param("tag", "Adventure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value("action-quest"));
    }

    @Test
    void gamesApiSortsAndClampsPage() throws Exception {
        mockMvc.perform(get("/api/games").param("sort", "rating").param("page", "99").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void curationsApiReturnsSections() throws Exception {
        mockMvc.perform(get("/api/games/curations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("popular"))
                .andExpect(jsonPath("$[0].items.length()").value(5));
    }

    @Test
    void facetsApiReturnsGlobalOptions() throws Exception {
        mockMvc.perform(get("/api/games/facets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres[0]").value("All"))
                .andExpect(jsonPath("$.genres", hasItem("캐주얼")))
                .andExpect(jsonPath("$.tags", hasItem("Single-player")));
    }

    @Test
    void detailAndSimilarApisReturnExpectedStatus() throws Exception {
        mockMvc.perform(get("/api/games/action-quest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Action Quest"));

        mockMvc.perform(get("/api/games/not-found"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/games/action-quest/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/games/not-found/similar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void similarApiReturnsOnlyStronglySimilarGames() throws Exception {
        gameRepository.deleteAll();
        gameRepository.saveAll(List.of(
                game(
                        "slash-hero",
                        "Slash Hero",
                        "액션",
                        List.of("Action", "Hack and Slash", "Single-player"),
                        "Fast melee action combat",
                        "Cut through stages with fast melee action combat."
                ),
                game(
                        "slash-sequel",
                        "Slash Sequel",
                        "액션",
                        List.of("Action", "Hack and Slash", "Single-player"),
                        "Fast melee action combat sequel",
                        "Cut through enemies with fast melee action combat."
                ),
                game(
                        "blade-adventure",
                        "Blade Adventure",
                        "액션",
                        List.of("Action", "Adventure", "Single-player"),
                        "Melee action adventure",
                        "Fight through action stages with sword combat."
                ),
                game(
                        "rogue-blade",
                        "Rogue Blade",
                        "인디",
                        List.of("Action", "Hack and Slash", "Roguelike"),
                        "Fast melee action combat roguelike",
                        "Repeat fast melee action combat runs."
                ),
                game(
                        "generic-action",
                        "Generic Action",
                        "액션",
                        List.of("Single-player", "Steam Achievements"),
                        "Generic action game",
                        "A generic game with platform features."
                ),
                game(
                        "rpg-adventure",
                        "RPG Adventure",
                        "RPG",
                        List.of("Adventure", "Single-player"),
                        "Fantasy adventure RPG",
                        "Explore a fantasy world."
                )
        ));

        mockMvc.perform(get("/api/games/slash-hero/similar").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("slash-sequel"))
                .andExpect(jsonPath("$[*].id", hasItem("blade-adventure")))
                .andExpect(jsonPath("$[*].id", hasItem("rogue-blade")))
                .andExpect(jsonPath("$[*].id", not(hasItem("slash-hero"))))
                .andExpect(jsonPath("$[*].id", not(hasItem("generic-action"))))
                .andExpect(jsonPath("$[*].id", not(hasItem("rpg-adventure"))));
    }

    @Test
    void adminSyncRequiresToken() throws Exception {
        mockMvc.perform(get("/api/admin/sync"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/sync").header("X-Admin-Token", "wrong-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/sync").header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/sync").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    private Games game(
            String id,
            String title,
            String genre,
            List<String> tags,
            String shortDescription,
            String description
    ) {
        return new Games(
                id,
                title,
                genre,
                tags,
                "https://example.com/" + id + ".jpg",
                4.5,
                50,
                "₩ 10,000",
                shortDescription,
                description,
                "https://store.steampowered.com/app/" + id
        );
    }
}
