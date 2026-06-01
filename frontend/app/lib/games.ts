export type Game = {
  id: string;
  title: string;
  genre: string;
  tags: string[];
  coverImageUrl: string;
  rating: number;
  popularity: number;
  reviewPositive: number;
  reviewNegative: number;
  reviewTotal: number;
  reviewScoreDescription: string;
  priceLabel: string;
  shortDescription: string;
  description: string;
  purchaseUrl: string;
};

export type GamePage = {
  items: Game[];
  totalCount: number;
  totalPages: number;
  page: number;
  size: number;
};

export type GameQueryParams = {
  q?: string;
  genre?: string;
  tag?: string;
  sort?: string;
  page?: number;
  size?: number;
};

export type GameCurationSection = {
  key: string;
  title: string;
  href: string;
  items: Game[];
};

export type GameFacets = {
  genres: string[];
  tags: string[];
};

const FALLBACK_COVER_IMAGE_URL = "/window.svg";

function getApiBase(): string {
  const base = process.env.API_BASE_URL;
  if (!base) {
    if (process.env.NODE_ENV === "production") {
      throw new Error("API_BASE_URL 환경변수가 설정되지 않았습니다.");
    }
    return "http://localhost:8080";
  }
  return base.replace(/\/+$/, "");
}

const EMPTY_PAGE: GamePage = {
  items: [],
  totalCount: 0,
  totalPages: 1,
  page: 1,
  size: 12,
};

export class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly path?: string,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

export function isApiRequestError(error: unknown): error is ApiRequestError {
  return error instanceof ApiRequestError;
}

export function hasKnownRating(rating: number) {
  return Number.isFinite(rating) && rating > 0;
}

export function formatRating(rating: number) {
  return hasKnownRating(rating) ? rating.toFixed(1).replace(/\.0$/, "") : "정보 없음";
}

export function hasSteamReviews(game: Pick<Game, "reviewTotal">) {
  return Number.isFinite(game.reviewTotal) && game.reviewTotal > 0;
}

export function getReviewPositivePercent(game: Pick<Game, "reviewPositive" | "reviewTotal">) {
  if (!Number.isFinite(game.reviewPositive) || !Number.isFinite(game.reviewTotal) || game.reviewTotal <= 0) {
    return 0;
  }
  return Math.round((game.reviewPositive / game.reviewTotal) * 100);
}

export function formatReviewCount(total: number) {
  return new Intl.NumberFormat("ko-KR").format(Math.max(0, total));
}

export function formatReviewScoreDescription(description: string) {
  const labels: Record<string, string> = {
    "Overwhelmingly Positive": "압도적으로 긍정적",
    "Very Positive": "매우 긍정적",
    Positive: "긍정적",
    "Mostly Positive": "대체로 긍정적",
    Mixed: "복합적",
    "Mostly Negative": "대체로 부정적",
    Negative: "부정적",
    "Very Negative": "매우 부정적",
    "Overwhelmingly Negative": "압도적으로 부정적",
    "No user reviews": "리뷰 없음",
  };

  return labels[description] ?? description;
}

export function formatReviewSummary(game: Pick<Game, "reviewPositive" | "reviewTotal">) {
  if (!hasSteamReviews(game)) return "리뷰 정보 없음";
  return `긍정 ${getReviewPositivePercent(game)}% · 리뷰 ${formatReviewCount(game.reviewTotal)}개`;
}

function buildApiUrl(path: string, params: Record<string, string | number | undefined>) {
  const base = getApiBase();
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "" && value !== "All") {
      searchParams.set(key, String(value));
    }
  });

  const query = searchParams.toString();
  return `${base}${path}${query ? `?${query}` : ""}`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object";
}

function asString(value: unknown, fallback = "") {
  return typeof value === "string" ? value : fallback;
}

function asNumber(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function asStringArray(value: unknown) {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}

function normalizeGame(value: unknown): Game | null {
  if (!isRecord(value)) return null;

  const id = asString(value.id).trim();
  if (!id) return null;

  return {
    id,
    title: asString(value.title, id),
    genre: asString(value.genre, "Unknown"),
    tags: asStringArray(value.tags),
    coverImageUrl: asString(value.coverImageUrl).trim() || FALLBACK_COVER_IMAGE_URL,
    rating: asNumber(value.rating),
    popularity: asNumber(value.popularity),
    reviewPositive: asNumber(value.reviewPositive),
    reviewNegative: asNumber(value.reviewNegative),
    reviewTotal: asNumber(value.reviewTotal),
    reviewScoreDescription: asString(value.reviewScoreDescription),
    priceLabel: asString(value.priceLabel, "Free"),
    shortDescription: asString(value.shortDescription),
    description: asString(value.description),
    purchaseUrl: asString(value.purchaseUrl, "#"),
  };
}

async function fetchJson(path: string, params: Record<string, string | number | undefined>) {
  const url = buildApiUrl(path, params);

  let res: Response;
  try {
    res = await fetch(url, {
      next: { revalidate: 60 },
    });
  } catch {
    throw new ApiRequestError("API 서버에 연결할 수 없습니다.", undefined, path);
  }

  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    throw new ApiRequestError(`API 요청 실패 (${res.status})`, res.status, path);
  }

  try {
    return await res.json();
  } catch {
    throw new ApiRequestError("API 응답을 해석할 수 없습니다.", res.status, path);
  }
}

function normalizePage(data: unknown): GamePage {
  if (Array.isArray(data)) {
    const items = data.map(normalizeGame).filter((game): game is Game => game !== null);
    return {
      items,
      totalCount: items.length,
      totalPages: 1,
      page: 1,
      size: items.length,
    };
  }

  if (!data || typeof data !== "object") {
    return EMPTY_PAGE;
  }

  const page = data as Partial<GamePage>;
  const items = Array.isArray(page.items)
    ? page.items.map(normalizeGame).filter((game): game is Game => game !== null)
    : [];
  const totalCount = asNumber(page.totalCount, items.length);
  const totalPages = asNumber(page.totalPages, 1);
  const currentPage = asNumber(page.page, 1);
  const pageSize = asNumber(page.size, 12);

  return {
    items,
    totalCount: Math.max(items.length, totalCount),
    totalPages: Math.max(1, totalPages),
    page: Math.max(1, currentPage),
    size: Math.max(1, pageSize),
  };
}

export async function fetchGamePage(params: GameQueryParams = {}): Promise<GamePage> {
  return normalizePage(await fetchJson("/api/games", params));
}

export async function fetchGames(): Promise<Game[]> {
  const page = await fetchGamePage({ page: 1, size: 500 });
  return page.items;
}

export async function fetchCurations(): Promise<GameCurationSection[]> {
  const data = await fetchJson("/api/games/curations", {});
  if (!Array.isArray(data)) return [];

  return data
    .filter(isRecord)
    .map((section) => ({
      key: asString(section.key),
      title: asString(section.title),
      href: asString(section.href, "/list"),
      items: Array.isArray(section.items)
        ? section.items.map(normalizeGame).filter((game): game is Game => game !== null)
        : [],
    }))
    .filter((section) => section.key && section.title && section.items.length > 0);
}

export async function fetchGameFacets(): Promise<GameFacets> {
  const data = await fetchJson("/api/games/facets", {});
  if (!isRecord(data)) {
    return { genres: ["All"], tags: [] };
  }

  return {
    genres: asStringArray(data.genres),
    tags: asStringArray(data.tags),
  };
}

export async function fetchGame(id: string): Promise<Game | null> {
  return normalizeGame(await fetchJson(`/api/games/${encodeURIComponent(id)}`, {}));
}

export async function fetchSimilarGames(id: string, limit = 3): Promise<Game[]> {
  const data = await fetchJson(`/api/games/${encodeURIComponent(id)}/similar`, { limit });
  return Array.isArray(data) ? data.map(normalizeGame).filter((game): game is Game => game !== null) : [];
}

export function getGenres(games: Game[]): string[] {
  return ["All", ...Array.from(new Set(games.map((game) => game.genre).filter(Boolean))).sort()];
}
