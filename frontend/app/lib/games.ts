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

export const genres = ["All", "RPG", "Action", "Strategy", "Simulation"] as const;

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

export async function fetchGames(): Promise<Game[]> {
  const base = getApiBase();
  const res = await fetch(`${base}/api/games`, {
    next: { revalidate: 60 },
  });
  if (!res.ok) return [];
  return res.json();
}

export function getSimilarGames(allGames: Game[], target: Game, limit = 3): Game[] {
  const allGenres = Array.from(new Set(allGames.map((g) => g.genre)));
  const allTags = Array.from(new Set(allGames.flatMap((g) => g.tags)));

  function toVector(game: Game): number[] {
    const genreVec = allGenres.map((g) => (game.genre === g ? 1 : 0));
    const tagVec = allTags.map((t) => (game.tags.includes(t) ? 1 : 0));
    return [...genreVec, ...tagVec];
  }

  function cosineSimilarity(a: number[], b: number[]): number {
    const dot = a.reduce((sum, v, i) => sum + v * b[i], 0);
    const magA = Math.sqrt(a.reduce((sum, v) => sum + v * v, 0));
    const magB = Math.sqrt(b.reduce((sum, v) => sum + v * v, 0));
    if (magA === 0 || magB === 0) return 0;
    return dot / (magA * magB);
  }

  const targetVec = toVector(target);

  return allGames
    .filter((g) => g.id !== target.id)
    .map((candidate) => ({
      candidate,
      similarity: cosineSimilarity(targetVec, toVector(candidate)),
    }))
    .filter((entry) => entry.similarity > 0)
    .sort((a, b) => b.similarity - a.similarity || b.candidate.popularity - a.candidate.popularity)
    .slice(0, limit)
    .map((entry) => entry.candidate);
}
