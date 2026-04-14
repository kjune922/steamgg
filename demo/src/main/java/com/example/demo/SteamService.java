package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    // 특정 게임 ID(AppID)를 주면 스팀에서 정보를 가져와 DB에 저장하는 함수
    public void fetchAndSaveGame(String appId) {
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
}