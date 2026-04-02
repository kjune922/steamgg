import Link from "next/link";
import { games } from "./lib/games";

export default function Home() {
  const popularGames = [...games].sort((a, b) => b.popularity - a.popularity).slice(0, 3);

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10">
      <section className="panel">
        <div className="flex items-center justify-between gap-4">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-600">
            steamgg MVP
          </p>
          <button
            type="button"
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800"
          >
            Login
          </button>
        </div>
        <h1 className="mt-5 text-3xl font-bold text-slate-900 sm:text-4xl">
          Find your next game faster.
        </h1>
        <p className="mt-3 max-w-2xl text-sm text-slate-600 sm:text-base">
          Search by title, move to filtered results, then open detail pages for description,
          purchase links, and similar picks.
        </p>
        <form action="/list" className="mt-6 flex flex-col gap-3 sm:flex-row">
          <input
            name="q"
            placeholder="Search games like Elden Ring"
            className="h-12 flex-1 rounded-xl border border-slate-300 bg-white px-4 text-sm text-slate-900"
          />
          <button
            type="submit"
            className="h-12 rounded-xl bg-slate-900 px-5 text-sm font-semibold text-white"
          >
            Search
          </button>
        </form>
      </section>

      <section className="mt-6">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Popular right now</h2>
          <Link href="/list?sort=popular" className="text-sm font-medium text-teal-700">
            View all
          </Link>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          {popularGames.map((game) => (
            <Link key={game.id} href={`/detail/${game.id}`} className="panel block p-4">
              <p className="text-xs text-slate-500">{game.genre}</p>
              <h3 className="mt-2 text-base font-semibold text-slate-900">{game.title}</h3>
              <p className="mt-2 text-sm text-slate-600">{game.shortDescription}</p>
              <p className="mt-3 text-xs font-semibold uppercase tracking-widest text-teal-700">
                Popularity {game.popularity}
              </p>
            </Link>
          ))}
        </div>
      </section>
    </main>
  );
}
