# Current Continuity

## 2026-04-18
- Backend security review follow-up was applied in `demo`, which is the current Spring Boot backend directory in this repo.
- `/api/admin/sync` now requires the `X-Admin-Api-Key` header to match `ADMIN_SYNC_API_KEY`; if the env var is empty, the endpoint returns 403.
- API CORS is configured through `APP_CORS_ALLOWED_ORIGINS`, defaulting to `http://localhost:3000`.
- `SteamService` now validates Steam app IDs as digits only, builds the Steam API URL through `UriComponentsBuilder`, and uses SLF4J logging instead of `System.out.println`.
- The DB password in `demo/src/main/resources/application.properties` was intentionally left unchanged per user request.

## Notes
- `docs/mvp/mvp-v1.md` was referenced by AGENTS.md but does not exist in the current workspace.
- There is no `backend/` directory in the current workspace; backend code is under `demo/`.
