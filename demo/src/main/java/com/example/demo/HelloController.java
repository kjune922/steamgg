package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class HelloController {

    private final SteamService steamService;
    private final GameService gameService;

    public HelloController(
            SteamService steamService,
            GameService gameService
    ) {
        this.steamService = steamService;
        this.gameService = gameService;
    }

    @GetMapping("/api/admin/sync")
    public String syncSteam() {
        int count = steamService.syncPopularGames();
        return "스팀 게임 " + count + " 개 동기화 완료";
    }

    @GetMapping("/api/games")
    public GamePageResponse getGames(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size
    ) {
        return gameService.searchGames(q, genre, tag, sort, page, size);
    }

    @GetMapping("/api/games/curations")
    public List<GameCurationSection> getCurations() {
        return gameService.getCurations();
    }

    @GetMapping("/api/games/facets")
    public GameFacets getFacets() {
        return gameService.getFacets();
    }

    @GetMapping("/api/games/{id}")
    public Games getGame(@PathVariable String id) {
        return gameService.getGameById(id);
    }

    @GetMapping("/api/games/{id}/similar")
    public List<Games> getSimilarGames(
            @PathVariable String id,
            @RequestParam(defaultValue = "3") Integer limit
    ) {
        return gameService.getSimilarGames(id, limit);
    }
}
