package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Games, String> {
    @Query(
            value = """
                    select distinct g
                    from Games g
                    left join g.tags tag
                    where (:genre is null or g.genre = :genre)
                      and (:tag is null or tag = :tag)
                      and (
                        :hasSearch = false
                        or lower(g.title) like concat('%', :search, '%')
                        or lower(g.genre) like concat('%', :search, '%')
                        or lower(g.shortDescription) like concat('%', :search, '%')
                        or lower(tag) like concat('%', :search, '%')
                        or replace(replace(replace(lower(g.title), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or replace(replace(replace(lower(g.genre), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or replace(replace(replace(lower(g.shortDescription), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or replace(replace(replace(lower(tag), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or (:hasAlias = true and g.genre = :aliasGenre)
                      )
                    """,
            countQuery = """
                    select count(distinct g)
                    from Games g
                    left join g.tags tag
                    where (:genre is null or g.genre = :genre)
                      and (:tag is null or tag = :tag)
                      and (
                        :hasSearch = false
                        or lower(g.title) like concat('%', :search, '%')
                        or lower(g.genre) like concat('%', :search, '%')
                        or lower(g.shortDescription) like concat('%', :search, '%')
                        or lower(tag) like concat('%', :search, '%')
                        or replace(replace(replace(lower(g.title), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or replace(replace(replace(lower(g.genre), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or replace(replace(replace(lower(g.shortDescription), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or replace(replace(replace(lower(tag), ' ', ''), '-', ''), '_', '') like concat('%', :normalizedSearch, '%')
                        or (:hasAlias = true and g.genre = :aliasGenre)
                      )
                    """
    )
    Page<Games> searchGames(
            @Param("hasSearch") boolean hasSearch,
            @Param("search") String search,
            @Param("normalizedSearch") String normalizedSearch,
            @Param("hasAlias") boolean hasAlias,
            @Param("aliasGenre") String aliasGenre,
            @Param("genre") String genre,
            @Param("tag") String tag,
            Pageable pageable
    );

    @Query("""
            select g.genre
            from Games g
            where g.genre is not null and g.genre <> ''
            group by g.genre
            order by count(g) desc, g.genre asc
            """)
    Page<String> findTopGenres(Pageable pageable);

    @Query("""
            select distinct g.genre
            from Games g
            where g.genre is not null and g.genre <> ''
            order by g.genre asc
            """)
    java.util.List<String> findDistinctGenres();

    @Query("""
            select distinct tag
            from Games g
            join g.tags tag
            where tag is not null and tag <> ''
            order by tag asc
            """)
    java.util.List<String> findDistinctTags();
}
