package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
public class HelloController {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 100;

    private final GameRepository gameRepository;
    private final SteamService steamService;
    private final String adminSyncToken;

    public HelloController(
            GameRepository gameRepository,
            SteamService steamService,
            @Value("${app.admin.sync-token:}") String adminSyncToken
    ) {
        this.gameRepository = gameRepository;
        this.steamService = steamService;
        this.adminSyncToken = adminSyncToken;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/api/admin/sync")
    public ResponseEntity<?> syncSteam(
            @RequestHeader(value = "X-Admin-Token", required = false) String headerToken,
            @RequestParam(defaultValue = "") String token
    ) {
        if (adminSyncToken == null || adminSyncToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin sync is disabled.");
        }
        if (!adminSyncToken.equals(headerToken) && !adminSyncToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid admin token.");
        }

        return ResponseEntity.ok(steamService.syncPopularGames());
    }

    @GetMapping("/api/games")
    public GamePageResponse getGames(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "All") String genre,
            @RequestParam(defaultValue = "") String tag,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int requestedPage = Math.max(1, page);
        Specification<Games> filters = gameFilters(q, genre, tag);
        Page<Games> result = gameRepository.findAll(
                filters,
                PageRequest.of(requestedPage - 1, safeSize, sortFor(sort))
        );

        int totalCount = Math.toIntExact(Math.min(Integer.MAX_VALUE, result.getTotalElements()));
        int totalPages = Math.max(1, result.getTotalPages());
        int safePage = totalCount == 0 ? 1 : Math.min(requestedPage, totalPages);

        if (safePage != requestedPage) {
            result = gameRepository.findAll(
                    filters,
                    PageRequest.of(safePage - 1, safeSize, sortFor(sort))
            );
        }

        return new GamePageResponse(
                result.getContent().stream().map(GameDto::from).toList(),
                totalCount,
                totalPages,
                safePage,
                safeSize
        );
    }

    @GetMapping("/api/games/facets")
    public GameFacetsResponse getGameFacets() {
        List<String> genres = gameRepository.findDistinctGenres().stream()
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> tags = gameRepository.findDistinctTags().stream()
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new GameFacetsResponse(genres, tags);
    }

    @GetMapping("/api/games/curations")
    public List<GameCurationSection> getGameCurations() {
        return List.of(
                new GameCurationSection(
                        "popular",
                        "인기 게임",
                        "/list?sort=popular",
                        gameRepository.findTop10ByOrderByPopularityDescTitleAsc().stream()
                                .map(GameDto::from)
                                .toList()
                ),
                new GameCurationSection(
                        "top-rated",
                        "평점 높은 게임",
                        "/list?sort=rating",
                        gameRepository.findTop10ByReviewTotalGreaterThanOrderByRatingDescReviewTotalDescTitleAsc(0).stream()
                                .map(GameDto::from)
                                .toList()
                ),
                new GameCurationSection(
                        "free",
                        "무료 게임",
                        "/list?q=Free",
                        gameRepository.findTop10ByPriceLabelIgnoreCaseOrderByPopularityDescTitleAsc("Free").stream()
                                .map(GameDto::from)
                                .toList()
                )
        ).stream().filter(section -> !section.items().isEmpty()).toList();
    }

    @GetMapping("/api/games/{id}")
    public ResponseEntity<GameDto> getGame(@PathVariable String id) {
        return gameRepository.findById(id)
                .map(GameDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/games/{id}/similar")
    public List<GameDto> getSimilarGames(
            @PathVariable String id,
            @RequestParam(defaultValue = "3") int limit
    ) {
        Optional<Games> current = gameRepository.findById(id);
        if (current.isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 12));
        Games base = current.get();
        List<String> tags = base.getTags() == null
                ? List.of()
                : base.getTags().stream()
                        .map(this::normalize)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
        Pageable topSimilar = PageRequest.of(
                0,
                safeLimit,
                Sort.by(Sort.Order.desc("popularity"), Sort.Order.asc("title"))
        );
        List<Games> similar = tags.isEmpty()
                ? gameRepository.findSimilarByGenre(base.getId(), nullToEmpty(base.getGenre()), topSimilar)
                : gameRepository.findSimilarByGenreOrTags(base.getId(), nullToEmpty(base.getGenre()), tags, topSimilar);

        return similar.stream()
                .map(GameDto::from)
                .toList();
    }

    private Specification<Games> gameFilters(String query, String genre, String tag) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (criteriaQuery != null) {
                criteriaQuery.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            String normalizedQuery = normalize(query);
            if (!normalizedQuery.isBlank()) {
                String likeQuery = "%" + normalizedQuery + "%";
                Join<Games, String> queryTags = root.join("tags", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("genre")), likeQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("shortDescription")), likeQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(queryTags), likeQuery)
                ));
            }

            String normalizedGenre = normalize(genre);
            if (!normalizedGenre.isBlank() && !"all".equals(normalizedGenre)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("genre")), normalizedGenre));
            }

            String normalizedTag = normalize(tag);
            if (!normalizedTag.isBlank()) {
                Join<Games, String> tags = root.join("tags", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(tags), normalizedTag));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort sortFor(String sort) {
        return switch (sort == null ? "popular" : sort) {
            case "rating" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("reviewTotal"), Sort.Order.asc("title"));
            case "title" -> Sort.by(Sort.Order.asc("title"));
            default -> Sort.by(Sort.Order.desc("popularity"), Sort.Order.asc("title"));
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record GameDto(
            String id,
            String title,
            String genre,
            List<String> tags,
            String coverImageUrl,
            double rating,
            int popularity,
            int reviewPositive,
            int reviewNegative,
            int reviewTotal,
            String reviewScoreDescription,
            String priceLabel,
            String shortDescription,
            String description,
            String purchaseUrl
    ) {
        static GameDto from(Games game) {
            return new GameDto(
                    nullToEmpty(game.getId()),
                    nullToEmpty(game.getTitle()),
                    nullToEmpty(game.getGenre()),
                    game.getTags() == null ? List.of() : game.getTags(),
                    nullToEmpty(game.getCoverImageUrl()),
                    game.getRating(),
                    game.getPopularity(),
                    game.getReviewPositive(),
                    game.getReviewNegative(),
                    game.getReviewTotal(),
                    nullToEmpty(game.getReviewScoreDescription()),
                    nullToEmpty(game.getPriceLabel()),
                    nullToEmpty(game.getShortDescription()),
                    nullToEmpty(game.getDescription()),
                    nullToEmpty(game.getPurchaseUrl())
            );
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    public record GamePageResponse(
            List<GameDto> items,
            int totalCount,
            int totalPages,
            int page,
            int size
    ) {
    }

    public record GameFacetsResponse(List<String> genres, List<String> tags) {
    }

    public record GameCurationSection(String key, String title, String href, List<GameDto> items) {
    }
}
