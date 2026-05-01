package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SteamService {

    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SteamService(GameRepository gameRepository, RestTemplate restTemplate) {
        this.gameRepository = gameRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper(); //1
    }
    @Scheduled(fixedDelay = 3600000)
    public void autoSyncSteamGames() {
        System.out.println(">>> [스케줄러] 정기 스팀 데이터 수집 시작");

        // 1. 인기 ID 목록 가져오기 (앞서 만든 메서드)
        List<String> popularIds = fetchPopularAppIds();

        int newCount = 0;
        for (String appId : popularIds) {
            // 2. 이미 있는 게임인지 체크 후 저장 (fetchAndSaveGame 내부에 existsById 로직 포함 필수)
            fetchAndSaveGame(appId);

            // 3. 스팀 서버 매너를 위해 0.5초씩 쉬어주기 (선택사항이지만 권장)
            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
        }

        System.out.println(">>> [스케줄러] 정기 수집 완료");
    }

    // 특정 게임 ID(AppID)를 주면 스팀에서 정보를 가져와 DB에 저장하는 함수
    public void fetchAndSaveGame(String appId) {
        if(gameRepository.existsById(appId)){
            return;
        }

        String url = "https://store.steampowered.com/api/appdetails?appids=" + appId + "&l=korean";

        try {
            // 2. 일단 문자열로 통째로 가져옴
            String jsonString = restTemplate.getForObject(url, String.class);

            // 3. 문자열을 JsonNode 트리구조로 변환
            JsonNode response = objectMapper.readTree(jsonString);
            if (response == null || !response.has(appId)) {
                System.out.println("스팀서버에서 응답이 없거나 잘못된 ID입니다.");
                return;
            }
            JsonNode root = response.get(appId);
            if (root.get("success").asBoolean()) {
                JsonNode data = root.get("data");

                if (data != null && data.get("type").asText().equals("game")) {
                    // 스팀 데이터를 우리 Games 엔티티로 변환
                    String title = data.get("name").asText();
                    String genre = data.get("genres").get(0).get("description").asText();
                    String coverUrl = data.get("header_image").asText();
                    String shortDesc = data.get("short_description").asText().replaceAll("<[^>]*>", ""); // HTML 태그 제거
                    String price = data.has("price_overview") ? data.get("price_overview").get("final_formatted").asText() : "Free";

                    List<String> tags = new ArrayList<>();
                    data.get("categories").forEach(cat -> tags.add(cat.get("description").asText()));

                    Games game = new Games(
                            appId, title, genre, tags, coverUrl,
                            4.5, 90, price, shortDesc, "Steam 자동 수집 데이터",
                            "https://store.steampowered.com/app/" + appId
                    );

                    gameRepository.save(game);
                    System.out.println("스팀에서 수집 완료: " + title);
                }
            }
        } catch (Exception e) {
            System.out.println("게임 수집 실패 (ID: " + appId + "): " + e.getMessage());
        }

    }

    public List<String> fetchPopularAppIds() {
        String url = "https://store.steampowered.com/api/featuredcategories";
        List<String> popularIds = new ArrayList<>();

        try {
            String jsonString = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonString);

            // 인기 신제품(new_releases) 섹션에서 게임 ID들을 추출
            JsonNode newReleases = root.get("new_releases").get("items");
            for (JsonNode item : newReleases) {
                popularIds.add(item.get("id").asText());
            }

            System.out.println("자동으로 찾은 게임 개수: " + popularIds.size());
        } catch (Exception e) {
            System.out.println("ID 목록 가져오기 실패: " + e.getMessage());
        }

        return popularIds;
    }
}