package com.example.demo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Games, String>, JpaSpecificationExecutor<Games> {

    @Override
    @EntityGraph(attributePaths = "tags")
    Page<Games> findAll(Specification<Games> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "tags")
    Optional<Games> findById(String id);

    @Query("select distinct g.genre from Games g where g.genre is not null and g.genre <> ''")
    List<String> findDistinctGenres();

    @Query("select distinct tag from Games g join g.tags tag where tag <> ''")
    List<String> findDistinctTags();

    @EntityGraph(attributePaths = "tags")
    List<Games> findTop10ByOrderByPopularityDescTitleAsc();

    @EntityGraph(attributePaths = "tags")
    List<Games> findTop10ByReviewTotalGreaterThanOrderByRatingDescReviewTotalDescTitleAsc(int reviewTotal);

    @EntityGraph(attributePaths = "tags")
    List<Games> findTop10ByPriceLabelIgnoreCaseOrderByPopularityDescTitleAsc(String priceLabel);

    @EntityGraph(attributePaths = "tags")
    @Query("""
            select distinct g from Games g
            left join g.tags tag
            where g.id <> :id
              and (lower(g.genre) = lower(:genre) or lower(tag) in :tags)
            """)
    List<Games> findSimilarByGenreOrTags(
            @Param("id") String id,
            @Param("genre") String genre,
            @Param("tags") List<String> tags,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "tags")
    @Query("""
            select g from Games g
            where g.id <> :id and lower(g.genre) = lower(:genre)
            """)
    List<Games> findSimilarByGenre(
            @Param("id") String id,
            @Param("genre") String genre,
            Pageable pageable
    );
}
