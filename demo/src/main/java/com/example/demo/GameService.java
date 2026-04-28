package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GameService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 500;
    private static final int CURATION_LIMIT = 6;
    private static final double GENRE_SIMILARITY_WEIGHT = 0.35;
    private static final double TAG_SIMILARITY_WEIGHT = 0.50;
    private static final double KEYWORD_SIMILARITY_WEIGHT = 0.15;
    private static final double MIN_SIMILARITY_SCORE = 0.40;
    private static final double MIN_CROSS_GENRE_TAG_SIMILARITY = 0.50;
    private static final double MIN_CROSS_GENRE_KEYWORD_SIMILARITY = 0.35;
    private static final double MIN_KEYWORD_ONLY_SIMILARITY = 0.35;
    private static final int MIN_CROSS_GENRE_MEANINGFUL_TAG_OVERLAP = 2;

    private static final Map<String, String> GENRE_ALIASES = Map.ofEntries(
            Map.entry("action", "액션"),
            Map.entry("액션", "액션"),
            Map.entry("adventure", "어드벤처"),
            Map.entry("어드벤처", "어드벤처"),
            Map.entry("casual", "캐주얼"),
            Map.entry("캐주얼", "캐주얼"),
            Map.entry("indie", "인디"),
            Map.entry("인디", "인디"),
            Map.entry("rpg", "RPG"),
            Map.entry("roleplaying", "RPG"),
            Map.entry("롤플레잉", "RPG"),
            Map.entry("simulation", "시뮬레이션"),
            Map.entry("sim", "시뮬레이션"),
            Map.entry("시뮬레이션", "시뮬레이션"),
            Map.entry("strategy", "전략"),
            Map.entry("전략", "전략")
    );

    private static final Map<String, Double> CORE_TAG_WEIGHTS = normalizedTagWeights(Map.ofEntries(
            Map.entry("4X", 1.5),
            Map.entry("Action", 1.25),
            Map.entry("Action RPG", 1.45),
            Map.entry("Base Building", 1.4),
            Map.entry("Card Battler", 1.45),
            Map.entry("Card Game", 1.4),
            Map.entry("City Builder", 1.4),
            Map.entry("Colony Sim", 1.45),
            Map.entry("Crafting", 1.25),
            Map.entry("CRPG", 1.45),
            Map.entry("Deckbuilding", 1.55),
            Map.entry("Exploration", 1.15),
            Map.entry("Farming", 1.35),
            Map.entry("Fantasy", 1.1),
            Map.entry("Fighting", 1.35),
            Map.entry("FPS", 1.45),
            Map.entry("Grand Strategy", 1.55),
            Map.entry("Hack and Slash", 1.45),
            Map.entry("Horror", 1.35),
            Map.entry("JRPG", 1.45),
            Map.entry("Life Sim", 1.4),
            Map.entry("Management", 1.3),
            Map.entry("Metroidvania", 1.55),
            Map.entry("Open World", 1.35),
            Map.entry("Platformer", 1.35),
            Map.entry("Puzzle", 1.35),
            Map.entry("Racing", 1.35),
            Map.entry("Rhythm", 1.35),
            Map.entry("Rogue-like", 1.55),
            Map.entry("Roguelike", 1.55),
            Map.entry("Sandbox", 1.3),
            Map.entry("Sci-fi", 1.15),
            Map.entry("Shooter", 1.35),
            Map.entry("Simulation", 1.25),
            Map.entry("Souls-like", 1.65),
            Map.entry("Sports", 1.35),
            Map.entry("Stealth", 1.35),
            Map.entry("Story Rich", 1.15),
            Map.entry("Strategy", 1.3),
            Map.entry("Survival", 1.45),
            Map.entry("Survival Horror", 1.55),
            Map.entry("Tactical RPG", 1.5),
            Map.entry("Third-Person Shooter", 1.45),
            Map.entry("Turn-Based", 1.45),
            Map.entry("Turn-Based Combat", 1.55),
            Map.entry("Visual Novel", 1.45)
    ));

    private static final Set<String> WEAK_TAGS = normalizedTagSet(List.of(
            "Co-op",
            "Free To Play",
            "Local Co-op",
            "Multi-player",
            "Multiplayer",
            "Online Co-op",
            "Online PvP",
            "PvP",
            "Single-player"
    ));

    private static final Set<String> IGNORED_TAGS = normalizedTagSet(List.of(
            "Captions available",
            "Family Sharing",
            "Full controller support",
            "Includes level editor",
            "Partial Controller Support",
            "Remote Play Together",
            "Shared/Split Screen",
            "Steam Achievements",
            "Steam Cloud",
            "Steam Leaderboards",
            "Steam Trading Cards",
            "Steam Workshop",
            "Valve Anti-Cheat enabled",
            "VR Support"
    ));

    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "again", "against", "also", "and", "are", "battle", "can", "for",
            "from", "game", "games", "into", "its", "new", "play", "player", "players", "steam",
            "that", "the", "this", "through", "with", "your"
    );

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public GamePageResponse searchGames(String q, String genre, String tag, String sort, Integer page, Integer size) {
        int safePage = page == null || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
        int safeSize = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));
        PageRequest pageRequest = PageRequest.of(safePage - 1, safeSize, sortFor(sort));

        String search = blankToNull(q);
        String normalizedSearch = search == null ? null : normalize(search);
        String aliasGenre = normalizedSearch == null ? null : GENRE_ALIASES.get(normalizedSearch);
        boolean hasSearch = search != null;
        boolean hasAlias = aliasGenre != null;
        String selectedGenre = normalizeFilter(genre);
        String selectedTag = normalizeFilter(tag);

        String lowerSearch = hasSearch ? search.toLowerCase(Locale.ROOT) : "";
        String normalizedSearchParam = hasSearch ? normalizedSearch : "";
        String aliasGenreParam = hasAlias ? aliasGenre : "";

        Page<Games> result = gameRepository.searchGames(
                hasSearch, lowerSearch, normalizedSearchParam,
                hasAlias, aliasGenreParam,
                selectedGenre, selectedTag,
                pageRequest
        );

        if (result.isEmpty() && result.getTotalElements() > 0 && result.getTotalPages() > 0) {
            pageRequest = PageRequest.of(result.getTotalPages() - 1, safeSize, sortFor(sort));
            result = gameRepository.searchGames(
                    hasSearch, lowerSearch, normalizedSearchParam,
                    hasAlias, aliasGenreParam,
                    selectedGenre, selectedTag,
                    pageRequest
            );
        }

        return toResponse(result);
    }

    public List<GameCurationSection> getCurations() {
        List<GameCurationSection> sections = new ArrayList<>();

        sections.add(section("popular", "인기 게임", "/list?sort=popular",
                searchGames(null, null, null, "popular", 1, CURATION_LIMIT).items()));
        sections.add(section("rating", "평점 높은 게임", "/list?sort=rating",
                searchGames(null, null, null, "rating", 1, CURATION_LIMIT).items()));
        sections.add(section("recent", "최근 추가된 게임", "/list",
                gameRepository.findAll(PageRequest.of(0, CURATION_LIMIT, Sort.by(Sort.Direction.DESC, "id"))).getContent()));

        List<String> genres = gameRepository.findTopGenres(PageRequest.of(0, CURATION_LIMIT)).getContent();
        for (String genre : genres) {
            List<Games> items = searchGames(null, genre, null, "popular", 1, CURATION_LIMIT).items();
            sections.add(section("genre-" + normalize(genre), genre, "/list?genre=" + encode(genre), items));
        }

        return sections.stream().filter(section -> !section.items().isEmpty()).toList();
    }

    public GameFacets getFacets() {
        List<String> genres = new ArrayList<>();
        genres.add("All");
        genres.addAll(gameRepository.findDistinctGenres());
        return new GameFacets(genres, gameRepository.findDistinctTags());
    }

    public Games getGameById(String id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    }

    public List<Games> getSimilarGames(String id, Integer limit) {
        Games target = getGameById(id);
        int safeLimit = limit == null ? 3 : Math.max(1, Math.min(limit, 12));
        List<Games> allGames = gameRepository.findAll();
        Map<String, Integer> tagDocumentCounts = tagDocumentCounts(allGames);
        Set<String> targetTags = normalizedTagsOf(target);

        return allGames.stream()
                .filter(candidate -> !candidate.getId().equals(target.getId()))
                .map(candidate -> scoreSimilarGame(target, targetTags, candidate, tagDocumentCounts, allGames.size()))
                .filter(SimilarGame::qualified)
                .sorted(Comparator
                        .comparingDouble(SimilarGame::similarity).reversed()
                        .thenComparing(SimilarGame::meaningfulTagOverlap, Comparator.reverseOrder())
                        .thenComparing(SimilarGame::tagSimilarity, Comparator.reverseOrder())
                        .thenComparing(entry -> entry.game().getTitle(), Comparator.nullsLast(String::compareTo)))
                .limit(safeLimit)
                .map(SimilarGame::game)
                .toList();
    }

    private GameCurationSection section(String key, String title, String href, List<Games> items) {
        return new GameCurationSection(key, title, href, items);
    }

    private GamePageResponse toResponse(Page<Games> result) {
        return new GamePageResponse(
                result.getContent(),
                result.getTotalElements(),
                Math.max(1, result.getTotalPages()),
                result.getNumber() + 1,
                result.getSize()
        );
    }

    private Sort sortFor(String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("rating"), Sort.Order.asc("title"));
        }
        if ("title".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.asc("title"));
        }
        return Sort.by(Sort.Order.desc("popularity"), Sort.Order.asc("title"));
    }

    private String normalizeFilter(String value) {
        String normalized = blankToNull(value);
        if (normalized == null || "All".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return normalizeTagKey(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private SimilarGame scoreSimilarGame(
            Games target,
            Set<String> targetTags,
            Games candidate,
            Map<String, Integer> tagDocumentCounts,
            int gameCount
    ) {
        boolean sameGenre = sameGenre(target, candidate);
        Set<String> candidateTags = normalizedTagsOf(candidate);
        int meaningfulTagOverlap = meaningfulTagOverlap(targetTags, candidateTags);
        double tagSimilarity = weightedTagSimilarity(targetTags, candidateTags, tagDocumentCounts, gameCount);
        double keywordSimilarity = keywordSimilarity(target, candidate);
        double similarity = (sameGenre ? GENRE_SIMILARITY_WEIGHT : 0)
                + (tagSimilarity * TAG_SIMILARITY_WEIGHT)
                + (keywordSimilarity * KEYWORD_SIMILARITY_WEIGHT);
        boolean hasCandidateSignal = sameGenre
                ? meaningfulTagOverlap > 0 || keywordSimilarity >= MIN_KEYWORD_ONLY_SIMILARITY
                : meaningfulTagOverlap >= MIN_CROSS_GENRE_MEANINGFUL_TAG_OVERLAP;
        boolean strongCrossGenreSignal = !sameGenre
                && meaningfulTagOverlap >= MIN_CROSS_GENRE_MEANINGFUL_TAG_OVERLAP
                && tagSimilarity >= MIN_CROSS_GENRE_TAG_SIMILARITY
                && keywordSimilarity >= MIN_CROSS_GENRE_KEYWORD_SIMILARITY;
        boolean qualified = hasCandidateSignal && (similarity >= MIN_SIMILARITY_SCORE || strongCrossGenreSignal);

        return new SimilarGame(candidate, similarity, meaningfulTagOverlap, tagSimilarity, qualified);
    }

    private List<String> tagsOf(Games game) {
        return game.getTags() == null ? List.of() : game.getTags();
    }

    private boolean sameGenre(Games target, Games candidate) {
        String targetGenre = canonicalGenre(target.getGenre());
        String candidateGenre = canonicalGenre(candidate.getGenre());
        return targetGenre != null && targetGenre.equals(candidateGenre);
    }

    private String canonicalGenre(String genre) {
        String normalized = blankToNull(genre);
        if (normalized == null) {
            return null;
        }
        String key = normalize(normalized);
        return GENRE_ALIASES.getOrDefault(key, normalized);
    }

    private Set<String> normalizedTagsOf(Games game) {
        Set<String> tags = new HashSet<>();
        for (String tag : tagsOf(game)) {
            String normalizedTag = normalizeTagKey(tag);
            if (!normalizedTag.isBlank()) {
                tags.add(normalizedTag);
            }
        }
        return tags;
    }

    private Map<String, Integer> tagDocumentCounts(List<Games> games) {
        Map<String, Integer> counts = new HashMap<>();
        for (Games game : games) {
            for (String tag : normalizedTagsOf(game)) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        return counts;
    }

    private int meaningfulTagOverlap(Set<String> targetTags, Set<String> candidateTags) {
        int overlap = 0;
        for (String tag : targetTags) {
            if (candidateTags.contains(tag) && isMeaningfulTag(tag)) {
                overlap++;
            }
        }
        return overlap;
    }

    private boolean isMeaningfulTag(String tag) {
        return baseTagWeight(tag) >= 0.75;
    }

    private double weightedTagSimilarity(
            Set<String> targetTags,
            Set<String> candidateTags,
            Map<String, Integer> tagDocumentCounts,
            int gameCount
    ) {
        Set<String> unionTags = new HashSet<>(targetTags);
        unionTags.addAll(candidateTags);

        double intersectionWeight = 0;
        double unionWeight = 0;
        for (String tag : unionTags) {
            double weight = tagWeight(tag, tagDocumentCounts, gameCount);
            if (weight == 0) {
                continue;
            }
            unionWeight += weight;
            if (targetTags.contains(tag) && candidateTags.contains(tag)) {
                intersectionWeight += weight;
            }
        }

        return unionWeight == 0 ? 0 : intersectionWeight / unionWeight;
    }

    private double tagWeight(String tag, Map<String, Integer> tagDocumentCounts, int gameCount) {
        double baseWeight = baseTagWeight(tag);
        if (baseWeight == 0) {
            return 0;
        }

        int documentCount = Math.max(1, tagDocumentCounts.getOrDefault(tag, 1));
        double inverseDocumentFrequency = Math.log((double) (gameCount + 1) / (documentCount + 1)) + 1;
        return baseWeight * inverseDocumentFrequency;
    }

    private double baseTagWeight(String tag) {
        if (IGNORED_TAGS.contains(tag)) {
            return 0;
        }
        if (WEAK_TAGS.contains(tag)) {
            return 0.2;
        }
        return CORE_TAG_WEIGHTS.getOrDefault(tag, 1.0);
    }

    private double keywordSimilarity(Games target, Games candidate) {
        Set<String> targetKeywords = keywordTokensOf(target);
        Set<String> candidateKeywords = keywordTokensOf(candidate);
        if (targetKeywords.isEmpty() || candidateKeywords.isEmpty()) {
            return 0;
        }

        Set<String> union = new HashSet<>(targetKeywords);
        union.addAll(candidateKeywords);

        int intersection = 0;
        for (String keyword : targetKeywords) {
            if (candidateKeywords.contains(keyword)) {
                intersection++;
            }
        }

        return (double) intersection / union.size();
    }

    private Set<String> keywordTokensOf(Games game) {
        String text = String.join(" ",
                game.getShortDescription() == null ? "" : game.getShortDescription(),
                game.getDescription() == null ? "" : game.getDescription()
        );
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private static String normalizeTagKey(String value) {
        if (value == null) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}]+", "");
    }

    private static Set<String> normalizedTagSet(List<String> values) {
        Set<String> normalizedValues = new HashSet<>();
        for (String value : values) {
            String normalizedValue = normalizeTagKey(value);
            if (!normalizedValue.isBlank()) {
                normalizedValues.add(normalizedValue);
            }
        }
        return Set.copyOf(normalizedValues);
    }

    private static Map<String, Double> normalizedTagWeights(Map<String, Double> weights) {
        Map<String, Double> normalizedWeights = new HashMap<>();
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String normalizedKey = normalizeTagKey(entry.getKey());
            if (!normalizedKey.isBlank()) {
                normalizedWeights.put(normalizedKey, entry.getValue());
            }
        }
        return Map.copyOf(normalizedWeights);
    }

    private record SimilarGame(
            Games game,
            double similarity,
            int meaningfulTagOverlap,
            double tagSimilarity,
            boolean qualified
    ) {
    }
}
