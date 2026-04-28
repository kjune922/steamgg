export type Game = {
  id: string;
  title: string;
  genre: string;
  tags: string[];
  coverImageUrl: string;
  rating: number;
  popularity: number;
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

function getApiBase(): string {
  const base = process.env.API_BASE_URL;
  if (!base) {
    if (process.env.NODE_ENV === "production") {
      throw new Error("API_BASE_URL 환경변수가 설정되지 않았습니다.");
    }
    return "http://localhost:8080";
  }
  return base;
}

const EMPTY_PAGE: GamePage = {
  items: [],
  totalCount: 0,
  totalPages: 1,
  page: 1,
  size: 12,
};

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

async function fetchJson(path: string, params: Record<string, string | number | undefined>) {
  try {
    const res = await fetch(buildApiUrl(path, params), {
      next: { revalidate: 60 },
    });
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

function normalizePage(data: unknown): GamePage {
  if (Array.isArray(data)) {
    return {
      items: data as Game[],
      totalCount: data.length,
      totalPages: 1,
      page: 1,
      size: data.length,
    };
  }

  if (!data || typeof data !== "object") {
    return EMPTY_PAGE;
  }

  const page = data as Partial<GamePage>;
  return {
    items: Array.isArray(page.items) ? page.items : [],
    totalCount: Number(page.totalCount ?? 0),
    totalPages: Math.max(1, Number(page.totalPages ?? 1)),
    page: Math.max(1, Number(page.page ?? 1)),
    size: Math.max(1, Number(page.size ?? 12)),
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
  return Array.isArray(data) ? data : [];
}

export async function fetchGameFacets(): Promise<GameFacets> {
  const data = await fetchJson("/api/games/facets", {});
  return {
    genres: Array.isArray(data?.genres) ? data.genres : ["All"],
    tags: Array.isArray(data?.tags) ? data.tags : [],
  };
}

export async function fetchGame(id: string): Promise<Game | null> {
  return fetchJson(`/api/games/${encodeURIComponent(id)}`, {}) as Promise<Game | null>;
}

export async function fetchSimilarGames(id: string, limit = 3): Promise<Game[]> {
  const data = await fetchJson(`/api/games/${encodeURIComponent(id)}/similar`, { limit });
  return Array.isArray(data) ? data : [];
}

export function getGenres(games: Game[]): string[] {
  return ["All", ...Array.from(new Set(games.map((game) => game.genre).filter(Boolean))).sort()];
}
