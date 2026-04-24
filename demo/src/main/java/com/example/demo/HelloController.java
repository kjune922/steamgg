package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Arrays;
import java.util.List;

/*
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class HelloController {

    private final GameRepository gameRepository;

    public HelloController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @GetMapping("/api/games")
    public List<Games> getGames() {
        // DB가 비어있다면 프론트엔드 데이터를 그대로 복사해서 넣음
        //if (gameRepository.count() == 0) {
            gameRepository.save(new Games("elden-ring", "Elden Ring", "RPG",
                    Arrays.asList("Open World", "Soulslike", "Fantasy"),
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/1245620/header.jpg",
                    4.8, 97, "$59.99", "Open-world action RPG.", "Full description here...",
                    "https://store.steampowered.com/app/1245620/ELDEN_RING/"));
            // 2. Balatro
            gameRepository.save(new Games("balatro", "Balatro", "Strategy",
                    Arrays.asList("Deckbuilder", "Roguelike", "Card Game"),
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/2379780/header.jpg",
                    4.7, 92, "$14.99", "Roguelike deckbuilder based on poker hands.", "Full description...",
                    "https://store.steampowered.com/app/2379780/Balatro/"));

            // 3. Cyberpunk 2077
            gameRepository.save(new Games("cyberpunk-2077", "Cyberpunk 2077", "Action",
                    Arrays.asList("Open World", "RPG", "Sci-Fi"),
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/1091500/header.jpg",
                    4.5, 89, "$59.99", "Narrative-heavy open-world action RPG.", "Full description...",
                    "https://store.steampowered.com/app/1091500/Cyberpunk_2077/"));

            // 4. Stardew Valley
            gameRepository.save(new Games("stardew-valley", "Stardew Valley", "Simulation",
                    Arrays.asList("Cozy", "Farming", "Pixel Art"),
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/413150/header.jpg",
                    4.9, 94, "$14.99", "Farming sim with crafting.", "Full description...",
                    "https://store.steampowered.com/app/413150/Stardew_Valley/"));
        //}
        return gameRepository.findAll();
    }

}*/ // 예전에 더미데이터로 돌리던 코드

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class HelloController {

    private final GameRepository gameRepository;
    private final SteamService steamService; // 서비스 추가

    public HelloController(GameRepository gameRepository, SteamService steamService) {
        this.gameRepository = gameRepository;
        this.steamService = steamService;
    }

    // 이 주소를 치면 지정된 스팀 ID의 게임들을 싹 긁어옴
    @GetMapping("/api/admin/sync")
    public String syncSteam() {
        // 테스트용 스팀 인기 게임 ID 리스트 (팰월드, 헬다이버즈2, 하이파이 러시, 더 파이널스, 철권, 그레이브 키퍼)
        // String[] targetAppIds = {"1623730", "553850", "1817230", "1966720", "1778820","599140","730"};
        List<String> targetAppIds = steamService.fetchPopularAppIds();
        int count = 0;
        for (String appId : targetAppIds) {
            count++;
            steamService.fetchAndSaveGame(appId);
        }

        // return "스팀 데이터 동기화 요청 완료!";
        return "스팀 게임 " + count + " 개 동기화 완료";
    }

    @GetMapping("/api/games")
    public List<Games> getGames() {
        return gameRepository.findAll();
    }


}
