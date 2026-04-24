package com.example.demo;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Games {
    @Id
    private String id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String genre;

    @ElementCollection // 간단한 문자열 리스트 저장
    private List<String> tags;

    @Column(columnDefinition = "TEXT")
    private String coverImageUrl;

    private double rating;
    private int popularity;

    @Column(columnDefinition = "TEXT")
    private String priceLabel;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT") // 긴 문장 저장용
    private String description;

    @Column(columnDefinition = "TEXT")
    private String purchaseUrl;

    // 기본 생성자 필수
    public Games() {}

    // 편의를 위한 생성자
// 모든 필드를 채우는 생성자 (데이터 입력 편의용)
    public Games(String id, String title, String genre, List<String> tags, String coverImageUrl,
                 double rating, int popularity, String priceLabel, String shortDescription,
                 String description, String purchaseUrl) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.tags = tags;
        this.coverImageUrl = coverImageUrl;
        this.rating = rating;
        this.popularity = popularity;
        this.priceLabel = priceLabel;
        this.shortDescription = shortDescription;
        this.description = description;
        this.purchaseUrl = purchaseUrl;
    }

    // Getter (프론트엔드 JSON 변환을 위해 필수)
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public List<String> getTags() { return tags; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public double getRating() { return rating; }
    public int getPopularity() { return popularity; }
    public String getPriceLabel() { return priceLabel; }
    public String getShortDescription() { return shortDescription; }
    public String getDescription() { return description; }
    public String getPurchaseUrl() { return purchaseUrl; }
}