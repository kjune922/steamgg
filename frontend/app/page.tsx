import Image from "next/image";
import Link from "next/link";
import { fetchGames } from "./lib/games";

const GENRES = ["RPG", "Action", "Strategy", "Simulation"] as const;

export default async function Home() {
  const games = await fetchGames();

  if (games.length === 0) {
    return (
      <main className="mx-auto flex w-full max-w-5xl flex-col items-center justify-center px-5 py-24 text-center">
        <p className="text-sm font-medium text-[#F5F7FA]">게임 데이터를 불러올 수 없습니다</p>
        <p className="mt-1 text-xs text-[#8A93A0]">잠시 후 다시 시도해주세요.</p>
      </main>
    );
  }

  const popularGames = [...games].sort((a, b) => b.popularity - a.popularity).slice(0, 6);
  const topRatedGames = [...games].sort((a, b) => b.rating - a.rating).slice(0, 6);
  const recentGames = [...games].slice(-6).reverse();
  const genreMap = Object.fromEntries(
    GENRES.map((g) => [g, games.filter((game) => game.genre === g).slice(0, 6)])
  );

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10 space-y-10">
      <CurationSection title="인기 게임" href="/list?sort=popular" games={popularGames} />
      <CurationSection title="평점 높은 게임" href="/list?sort=rating" games={topRatedGames} />
      <CurationSection title="최근 추가된 게임" href="/list" games={recentGames} />
      {GENRES.map((genre) =>
        genreMap[genre].length > 0 ? (
          <CurationSection
            key={genre}
            title={genre}
            href={`/list?genre=${encodeURIComponent(genre)}`}
            games={genreMap[genre]}
          />
        ) : null
      )}
    </main>
  );
}

function CurationSection({
  title,
  href,
  games,
}: {
  title: string;
  href: string;
  games: Awaited<ReturnType<typeof fetchGames>>;
}) {
  if (games.length === 0) return null;

  return (
    <section aria-label={title}>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-base font-semibold text-[#F5F7FA]">{title}</h2>
        <Link
          href={href}
          className="cursor-pointer text-xs font-medium text-[#E60023] transition-colors hover:text-[#C4001E]"
        >
          더 보기 →
        </Link>
      </div>
      <div className="scroll-row">
        {games.map((game) => (
          <Link
            key={game.id}
            href={`/detail/${game.id}`}
            prefetch={false}
            className="panel panel-hover flex min-w-[160px] flex-col overflow-hidden sm:min-w-[180px]"
          >
            <div className="relative h-24 w-full flex-shrink-0 sm:h-28">
              <Image
                src={game.coverImageUrl}
                alt={`${game.title} 커버 이미지`}
                fill
                sizes="180px"
                className="object-cover"
              />
            </div>
            <div className="flex flex-1 flex-col p-3">
              <p className="text-xs text-[#8A93A0]">{game.genre}</p>
              <h3 className="mt-1 line-clamp-1 text-sm font-semibold text-[#F5F7FA]">
                {game.title}
              </h3>
              <p className="mt-auto pt-2 text-xs font-medium text-[#B6BEC9]">
                {game.priceLabel}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
