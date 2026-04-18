# Frontend MVP Code Map

## Scope
- `frontend/app/page.tsx`: ROOT page (search entry, popular games, login button UI)
- `frontend/app/list/page.tsx`: LIST page (search results, filter, sort with query params)
- `frontend/app/detail/[id]/page.tsx`: DETAIL page (description, purchase link, similar games)
- `frontend/app/lib/games.ts`: shared game mock data and lookup helpers

## Data Flow
1. ROOT search form submits `q` to `/list`.
2. LIST reads `q`, `genre`, `sort` from URL and computes filtered/sorted games.
3. DETAIL reads route param `id` and resolves game + similar games from shared data.

## Notes
- Current MVP is frontend-only with local mock dataset.
- No auth flow or deployment configuration changes included.
