# API Spec

## 1. 문서 개요

이 문서는 MVP v1 기준 API 명세를 정리한다.

현재 MVP 범위는 아래 3개 페이지를 지원하는 데 목적이 있다.

- ROOT
- LIST
- DETAIL

현재 포함 API는 아래와 같다.

- GET /api/v1/games/popular
- GET /api/v1/filters
- GET /api/v1/games
- GET /api/v1/games/{gameId}
- GET /api/v1/games/{gameId}/similar

현재 범위에서 제외하는 API는 아래와 같다.

- 로그인
- 회원가입
- 리뷰
- 즐겨찾기
- 관리자 기능
- AI 추천
- 디스코드 봇 연동

---

## 2. 공통 규칙

### 2.1 Base URL
- /api/v1

### 2.2 응답 형식 원칙
- 프론트에서 바로 사용할 수 있도록 단순한 JSON 구조를 유지한다.
- 엔티티 전체를 그대로 노출하지 않고, 페이지 요구사항에 맞는 DTO 형태로 응답한다.

### 2.3 정렬 기준
LIST 페이지에서 사용하는 정렬 값은 아래를 사용한다.

- popularity
- players
- latest
- title

### 2.4 페이지네이션
LIST API는 페이지네이션을 지원한다.

기본 규칙
- page 기본값: 0
- size 기본값: 20

---

## 3. API 목록 요약

### 3.1 ROOT
- GET /api/v1/games/popular

### 3.2 LIST
- GET /api/v1/filters
- GET /api/v1/games

### 3.3 DETAIL
- GET /api/v1/games/{gameId}
- GET /api/v1/games/{gameId}/similar

---

## 4. API 상세 명세

## 4.1 인기 게임 조회

### 목적
ROOT 페이지에서 인기 게임 일부를 노출하기 위해 사용한다.

### Endpoint
- GET /api/v1/games/popular

### Query Parameters

| 이름 | 타입 | 필수 여부 | 기본값 | 설명 |
|---|---|---|---|---|
| limit | number | 아니오 | 8 | 반환할 인기 게임 수 |

### 정렬 기준
- popularity_score DESC

### 응답 예시

```json
{
  "games": [
    {
      "id": 1,
      "steamAppId": 730,
      "title": "Counter-Strike 2",
      "capsuleImageUrl": "https://example.com/cs2.jpg",
      "currentPlayers": 945321,
      "popularityScore": 98.4
    },
    {
      "id": 2,
      "steamAppId": 578080,
      "title": "PUBG: BATTLEGROUNDS",
      "capsuleImageUrl": "https://example.com/pubg.jpg",
      "currentPlayers": 423210,
      "popularityScore": 95.2
    }
  ]
}
```

### 사용 페이지
- ROOT

---

## 4.2 필터 메타데이터 조회

### 목적
LIST 페이지에서 장르, 태그 필터 UI를 구성하기 위해 사용한다.

### Endpoint
- GET /api/v1/filters

### Query Parameters
- 없음

### 응답 예시

```json
{
  "genres": [
    { "id": 1, "name": "FPS" },
    { "id": 2, "name": "RPG" },
    { "id": 3, "name": "Survival" }
  ],
  "tags": [
    { "id": 1, "name": "Hardcore" },
    { "id": 2, "name": "Co-op" },
    { "id": 3, "name": "Tactical" }
  ]
}
```

### 사용 페이지
- LIST

---

## 4.3 게임 목록 조회

### 목적
LIST 페이지에서 검색어, 필터, 정렬 기준으로 게임 목록을 조회한다.

### Endpoint
- GET /api/v1/games

### Query Parameters

| 이름 | 타입 | 필수 여부 | 기본값 | 설명 |
|---|---|---|---|---|
| query | string | 아니오 |  | 게임명 검색어 |
| genre | string | 아니오 |  | 장르명 |
| tag | string | 아니오 |  | 태그명 |
| players | number | 아니오 |  | 플레이 인원 |
| sort | string | 아니오 | popularity | 정렬 기준 |
| page | number | 아니오 | 0 | 페이지 번호 |
| size | number | 아니오 | 20 | 페이지 크기 |

### 필터 규칙
- query는 title 기준 부분 검색으로 처리한다.
- genre는 해당 장르를 가진 게임만 조회한다.
- tag는 해당 태그를 가진 게임만 조회한다.
- players는 아래 조건으로 처리한다.
    - min_players <= players <= max_players

### 정렬 규칙
- popularity: popularity_score DESC
- players: current_players DESC
- latest: release_date DESC
- title: title ASC

### 요청 예시
- /api/v1/games?query=tarkov&genre=FPS&tag=Hardcore&players=3&sort=popularity&page=0&size=20

### 응답 예시

```json
{
  "content": [
    {
      "id": 15,
      "steamAppId": 12345,
      "title": "Escape from Tarkov",
      "shortDescription": "하드코어 전술형 extraction FPS",
      "capsuleImageUrl": "https://example.com/tarkov.jpg",
      "minPlayers": 1,
      "maxPlayers": 5,
      "currentPlayers": 58421,
      "popularityScore": 93.1,
      "genres": ["FPS", "Extraction"],
      "tags": ["Hardcore", "Tactical"]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 사용 페이지
- LIST

---

## 4.4 게임 상세 조회

### 목적
DETAIL 페이지에서 특정 게임의 상세 정보를 조회한다.

### Endpoint
- GET /api/v1/games/{gameId}

### Path Parameters

| 이름 | 타입 | 설명 |
|---|---|---|
| gameId | number | 내부 게임 ID |

### 응답 예시

```json
{
  "id": 15,
  "steamAppId": 12345,
  "title": "Escape from Tarkov",
  "shortDescription": "하드코어 전술형 extraction FPS",
  "headerImageUrl": "https://example.com/tarkov-header.jpg",
  "capsuleImageUrl": "https://example.com/tarkov-capsule.jpg",
  "purchaseUrl": "https://store.steampowered.com/app/12345",
  "minPlayers": 1,
  "maxPlayers": 5,
  "currentPlayers": 58421,
  "popularityScore": 93.1,
  "releaseDate": "2023-01-01",
  "isFree": false,
  "developer": "Battlestate Games",
  "publisher": "Battlestate Games",
  "genres": ["FPS", "Extraction", "Survival"],
  "tags": ["Hardcore", "Tactical", "Loot"]
}
```

### 사용 페이지
- DETAIL

---

## 4.5 유사 게임 조회

### 목적
DETAIL 페이지에서 현재 보고 있는 게임과 비슷한 게임을 노출한다.

### Endpoint
- GET /api/v1/games/{gameId}/similar

### Path Parameters

| 이름 | 타입 | 설명 |
|---|---|---|
| gameId | number | 내부 게임 ID |

### Query Parameters

| 이름 | 타입 | 필수 여부 | 기본값 | 설명 |
|---|---|---|---|---|
| limit | number | 아니오 | 6 | 반환할 유사 게임 수 |

### 유사 게임 기준
현재 MVP에서는 아래 기준으로 계산한다.

- 같은 장르 수
- 같은 태그 수
- 인원수 범위 유사성
- popularity_score 보정

### 추천 가중치 예시
- 같은 장르: +2
- 같은 태그: +1
- 인원수 범위 유사: +1

### 응답 예시

```json
{
  "games": [
    {
      "id": 17,
      "title": "Hunt: Showdown",
      "capsuleImageUrl": "https://example.com/hunt.jpg",
      "currentPlayers": 18542,
      "genres": ["FPS", "Extraction"],
      "tags": ["Tactical", "Hardcore"]
    }
  ]
}
```

### 사용 페이지
- DETAIL

---

## 5. 페이지별 API 매핑

## 5.1 ROOT
- GET /api/v1/games/popular

## 5.2 LIST
- GET /api/v1/filters
- GET /api/v1/games

## 5.3 DETAIL
- GET /api/v1/games/{gameId}
- GET /api/v1/games/{gameId}/similar

---

## 6. 현재 범위 밖 확장 가능한 API

아래 API는 현재 MVP 범위에는 포함하지 않지만 추후 확장 후보로 본다.

- POST /api/v1/auth/login
- GET /api/v1/users/favorites
- POST /api/v1/users/favorites
- GET /api/v1/admin/games
- POST /api/v1/admin/games
- POST /api/v1/recommend

---

## 7. 주의사항

- 현재 MVP에서는 Steam API를 프론트에서 직접 호출하지 않는다.
- 검색과 필터는 우리 DB 기준으로 처리한다.
- 현재 응답 형식은 3개 페이지 구현에 필요한 최소 필드 중심으로 유지한다.
- 요청이 없으면 범위를 넘어가는 필드나 기능을 추가하지 않는다.