# Current Continuity

## Date
- 2026-04-02

## Active Goals
- Build frontend MVP flow for ROOT -> LIST -> DETAIL.
- Keep changes limited to frontend and docs continuity/code-map.

## Current State
- ROOT (`frontend/app/page.tsx`): search input, popular game cards, login button UI implemented.
- LIST (`frontend/app/list/page.tsx`): query-param based search/filter/sort implemented and query normalization tightened.
- DETAIL (`frontend/app/detail/[id]/page.tsx`): title/description/image/tags/purchase link/similar games implemented.
- Shared mock data and similar-game helper in `frontend/app/lib/games.ts`.
- `frontend/README.md`: portfolio-oriented Korean explanation added for page design intent and structure choices.
- DB/ERD baseline updated to MVP core fields (`games`, `genres`, `tags`, join tables).
- Backend `GameResponse` now includes description/image/purchase/players/popularity/genres/tags fields.

## Decisions
- Frontend-only implementation for MVP pages; backend contract extension deferred.
- No new libraries added.
- Auth flow and infra/deployment configs unchanged.

## Validation
- `npm run lint` in `frontend` passed.
- `npm run build` in `frontend` passed.
- `./gradlew.bat test` in `backend` passed.
- ROOT acceptance re-check completed on 2026-04-02:
- Search input visible, popular game cards visible, login button UI visible.
- Popular cards link to `/detail/[id]` and search form routes to `/list?q=...`.
- LIST acceptance re-check completed on 2026-04-02:
- Query -> result filter works, genre filter and sort change result ordering.
- URL query and form defaults are aligned for `q`, `genre`, `sort`.
- DETAIL acceptance re-check completed on 2026-04-02:
- Detail data, purchase link, and similar game cards are rendered.
- Similar cards navigate to `/detail/[id]` links.
- ERD/schema mapping re-check completed on 2026-04-02:
- ROOT/LIST/DETAIL required fields are mapped in `docs/erd/erd.md`.

## Known Gaps
- Data source is local mock data, not backend API.
- `/api/v1` 상세/유사/필터 메타데이터 endpoint 구현은 아직 미완료.
- `next/image` optimization is not yet applied for remote covers (`img` lint warnings only).
- `docs/mvp/mvp-v1.md` 파일이 없어 루트 AGENTS의 해당 참조는 확인 불가.

## Next Actions
- Implement `/api/v1/games/popular`, `/api/v1/games/{id}`, `/api/v1/games/{id}/similar`, `/api/v1/filters`.
- Replace mock data with API fetch once backend endpoints are ready.
- Define whether API route should stay `/api/games` or be standardized to `/api/v1/games`.
- Commit and push the frontend README documentation update if approved.

## Next Files To Check
- `backend/src/main/java/com/steamgg/backend/game/GameResponse.java`
- `backend/src/main/java/com/steamgg/backend/game/GameController.java`
- `backend/src/main/java/com/steamgg/backend/game/Game.java`
- `backend/src/main/java/com/steamgg/backend/game/Genre.java`
- `backend/src/main/java/com/steamgg/backend/game/Tag.java`
- `infra/init.sql`
- `docs/erd/erd.md`
- `docs/db-schema.md`
- `frontend/app/lib/games.ts`
- `frontend/app/list/page.tsx`
- `frontend/app/detail/[id]/page.tsx`
- `frontend/app/page.tsx`
- `frontend/next.config.ts` (if moving cover images to `next/image`)
