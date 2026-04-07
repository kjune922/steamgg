import Link from "next/link";
import { games } from "./lib/games";

export default function Home() {
  const popularGames = [...games].sort((a, b) => b.popularity - a.popularity).slice(0, 4);

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-14 sm:px-8 sm:py-20">
      {/* Hero: search first */}
      <section className="flex flex-col items-center text-center">
        <h1 className="text-3xl font-bold text-[#F5F7FA] sm:text-4xl">
          Find your next game
        </h1>
        <p className="mt-3 text-sm text-[#8A93A0]">
          Search by title and discover games instantly
        </p>
        <form action="/list" className="mt-8 flex w-full max-w-xl gap-2">
          <input
            name="q"
            placeholder="Search games like Elden Ring..."
            className="h-12 flex-1 rounded-xl border border-[#2A313C] bg-[#181C22] px-4 text-sm text-[#F5F7FA] placeholder:text-[#8A93A0] focus:border-[#E60023] focus:outline-none focus:ring-1 focus:ring-[#E60023] transition-colors"
          />
          <button
            type="submit"
            className="h-12 rounded-xl bg-[#E60023] px-5 text-sm font-semibold text-white transition-colors hover:bg-[#C4001E]"
          >
            Search
          </button>
        </form>
      </section>

      {/* Popular games grid */}
      <section className="mt-16">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-[#F5F7FA]">Popular right now</h2>
          <Link
            href="/list?sort=popular"
            className="text-sm font-medium text-[#E60023] transition-colors hover:text-[#C4001E]"
          >
            View all →
          </Link>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {popularGames.map((game) => (
            <Link
              key={game.id}
              href={`/detail/${game.id}`}
              className="panel panel-hover block overflow-hidden"
            >
              <img
                src={game.coverImageUrl}
                alt={`${game.title} cover`}
                className="h-32 w-full object-cover"
              />
              <div className="p-3">
                <p className="text-xs text-[#8A93A0]">{game.genre}</p>
                <h3 className="mt-1 text-sm font-semibold leading-tight text-[#F5F7FA]">
                  {game.title}
                </h3>
                <p className="mt-1 text-xs text-[#B6BEC9]">{game.priceLabel}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </main>
  );
}
