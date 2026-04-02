export type Game = {
  id: string;
  title: string;
  genre: "RPG" | "Action" | "Strategy" | "Simulation";
  tags: string[];
  coverImageUrl: string;
  rating: number;
  popularity: number;
  priceLabel: string;
  shortDescription: string;
  description: string;
  purchaseUrl: string;
};

export const games: Game[] = [
  {
    id: "elden-ring",
    title: "Elden Ring",
    genre: "RPG",
    tags: ["Open World", "Soulslike", "Fantasy"],
    coverImageUrl:
      "https://cdn.cloudflare.steamstatic.com/steam/apps/1245620/header.jpg",
    rating: 4.8,
    popularity: 97,
    priceLabel: "$59.99",
    shortDescription: "Open-world action RPG with deep boss encounters.",
    description:
      "Explore a vast dark-fantasy world, build your class, and challenge high-stakes bosses with flexible combat styles.",
    purchaseUrl: "https://store.steampowered.com/app/1245620/ELDEN_RING/",
  },
  {
    id: "balatro",
    title: "Balatro",
    genre: "Strategy",
    tags: ["Deckbuilder", "Roguelike", "Card Game"],
    coverImageUrl:
      "https://cdn.cloudflare.steamstatic.com/steam/apps/2379780/header.jpg",
    rating: 4.7,
    popularity: 92,
    priceLabel: "$14.99",
    shortDescription: "Roguelike deckbuilder based on poker hands.",
    description:
      "Build absurdly strong combinations with jokers and modifiers to push high-score runs in short sessions.",
    purchaseUrl: "https://store.steampowered.com/app/2379780/Balatro/",
  },
  {
    id: "cyberpunk-2077",
    title: "Cyberpunk 2077",
    genre: "Action",
    tags: ["Open World", "RPG", "Sci-Fi"],
    coverImageUrl:
      "https://cdn.cloudflare.steamstatic.com/steam/apps/1091500/header.jpg",
    rating: 4.5,
    popularity: 89,
    priceLabel: "$59.99",
    shortDescription: "Narrative-heavy open-world action RPG.",
    description:
      "Take contracts in Night City, customize your build, and progress through branching quests with cinematic presentation.",
    purchaseUrl:
      "https://store.steampowered.com/app/1091500/Cyberpunk_2077/",
  },
  {
    id: "stardew-valley",
    title: "Stardew Valley",
    genre: "Simulation",
    tags: ["Cozy", "Farming", "Pixel Art"],
    coverImageUrl:
      "https://cdn.cloudflare.steamstatic.com/steam/apps/413150/header.jpg",
    rating: 4.9,
    popularity: 94,
    priceLabel: "$14.99",
    shortDescription: "Farming sim with crafting and social life.",
    description:
      "Restore your farm, build relationships, and plan seasons of crops, fishing, mining, and town events.",
    purchaseUrl: "https://store.steampowered.com/app/413150/Stardew_Valley/",
  },
  {
    id: "civilization-vi",
    title: "Civilization VI",
    genre: "Strategy",
    tags: ["Turn-Based", "4X", "Empire Building"],
    coverImageUrl:
      "https://cdn.cloudflare.steamstatic.com/steam/apps/289070/header.jpg",
    rating: 4.4,
    popularity: 83,
    priceLabel: "$59.99",
    shortDescription: "Turn-based empire building strategy.",
    description:
      "Lead a civilization from ancient to modern eras through diplomacy, science, war, and cultural development.",
    purchaseUrl:
      "https://store.steampowered.com/app/289070/Sid_Meiers_Civilization_VI/",
  },
];

export const genres = ["All", "RPG", "Action", "Strategy", "Simulation"] as const;

export function findGameById(id: string) {
  return games.find((game) => game.id === id);
}

export function getSimilarGames(target: Game, limit = 3) {
  return games
    .filter((candidate) => candidate.id !== target.id)
    .map((candidate) => {
      const sharedTags = candidate.tags.filter((tag) => target.tags.includes(tag)).length;
      const sameGenreScore = candidate.genre === target.genre ? 2 : 0;
      const score = sameGenreScore + sharedTags;
      return { candidate, score };
    })
    .filter((entry) => entry.score > 0)
    .sort((a, b) => b.score - a.score || b.candidate.popularity - a.candidate.popularity)
    .slice(0, limit)
    .map((entry) => entry.candidate);
}
