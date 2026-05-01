# DB Schema (MVP v1)

## Scope
3페이지 MVP(ROOT/LIST/DETAIL)에 필요한 최소 스키마만 유지한다.

## Core Tables
### `games`
| column | type | nullable | note |
|---|---|---|---|
| `id` | `bigserial` | N | PK |
| `steam_app_id` | `bigint` | N | UNIQUE, Steam source key |
| `title` | `varchar(255)` | N | 검색 기준 |
| `short_description` | `text` | Y | 상세 설명 |
| `header_image_url` | `text` | Y | 상세 대표 이미지 |
| `capsule_image_url` | `text` | Y | 목록/카드 이미지 |
| `purchase_url` | `text` | Y | 구매 링크 |
| `current_players` | `integer` | Y | 인원수 정보 |
| `popularity_score` | `numeric(5,2)` | Y | 인기 정렬값 |
| `created_at` | `timestamp` | N | 생성 시각 |
| `updated_at` | `timestamp` | N | 수정 시각 |

### `genres`
| column | type | nullable | note |
|---|---|---|---|
| `id` | `bigserial` | N | PK |
| `name` | `varchar(100)` | N | UNIQUE |
| `created_at` | `timestamp` | N | 생성 시각 |

### `tags`
| column | type | nullable | note |
|---|---|---|---|
| `id` | `bigserial` | N | PK |
| `name` | `varchar(100)` | N | UNIQUE |
| `created_at` | `timestamp` | N | 생성 시각 |

### `game_genres`
| column | type | nullable | note |
|---|---|---|---|
| `id` | `bigserial` | N | PK |
| `game_id` | `bigint` | N | FK -> `games.id` |
| `genre_id` | `bigint` | N | FK -> `genres.id` |

Constraint:
- `UNIQUE(game_id, genre_id)`

### `game_tags`
| column | type | nullable | note |
|---|---|---|---|
| `id` | `bigserial` | N | PK |
| `game_id` | `bigint` | N | FK -> `games.id` |
| `tag_id` | `bigint` | N | FK -> `tags.id` |

Constraint:
- `UNIQUE(game_id, tag_id)`

## Removed Columns (Non-MVP)
아래 컬럼은 MVP 3페이지 요구사항 대비 우선순위가 낮아 기본 스키마에서 제외:
- `min_players`, `max_players`
- `release_date`
- `is_free`
- `developer`, `publisher`

## Indexes
- `games(title)`
- `games(current_players)`
- `games(popularity_score)`
- `game_genres(genre_id)`
- `game_tags(tag_id)`

## API Mapping Quick Check
- ROOT: `id`, `steam_app_id`, `title`, `capsule_image_url`, `current_players`, `popularity_score`
- LIST: ROOT 필드 + `genres`, `tags`
- DETAIL: `title`, `short_description`, `header_image_url`, `purchase_url`, `genres`, `tags`, `current_players`, `popularity_score`
