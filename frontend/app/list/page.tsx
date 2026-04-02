import Link from "next/link";
import { games, genres } from "../lib/games";

type ListPageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

function asValue(input: string | string[] | undefined) {
  if (Array.isArray(input)) {
    return input[0] ?? "";
  }
  return input ?? "";
}

const sortOptions = ["popular", "rating", "title"] as const;

export default async function ListPage({ searchParams }: ListPageProps) {
  const params = (await searchParams) ?? {};
  const rawQuery = asValue(params.q).trim();
  const query = rawQuery.toLowerCase();
  const genreParam = asValue(params.genre);
  const sortParam = asValue(params.sort);
  const genre = genres.includes(genreParam as (typeof genres)[number]) ? genreParam : "All";
  const sort = sortOptions.includes(sortParam as (typeof sortOptions)[number])
    ? sortParam
    : "popular";

  let filtered = games.filter((game) => {
    const matchesQuery = query.length === 0 || game.title.toLowerCase().includes(query);
    const matchesGenre = genre === "All" || game.genre === genre;
    return matchesQuery && matchesGenre;
  });

  filtered = filtered.sort((a, b) => {
    if (sort === "rating") {
      return b.rating - a.rating;
    }
    if (sort === "title") {
      return a.title.localeCompare(b.title);
    }
    return b.popularity - a.popularity;
  });

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10">
      <section className="panel">
        <h1 className="text-2xl font-bold text-slate-900">Game list</h1>
        <p className="mt-2 text-sm text-slate-600">
          Search results with genre filter and sorting.
        </p>

        <form className="mt-5 grid gap-3 sm:grid-cols-4">
          <input
            name="q"
            defaultValue={rawQuery}
            placeholder="Search title"
            className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm sm:col-span-2"
          />
          <select
            name="genre"
            defaultValue={genre}
            className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm"
          >
            {genres.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
          <select
            name="sort"
            defaultValue={sort}
            className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm"
          >
            <option value="popular">Popular</option>
            <option value="rating">Rating</option>
            <option value="title">Title</option>
          </select>
          <button
            type="submit"
            className="h-11 rounded-xl bg-slate-900 px-4 text-sm font-semibold text-white sm:col-span-4 sm:w-32"
          >
            Apply
          </button>
        </form>
      </section>

      <section className="mt-6 grid gap-3">
        {filtered.length === 0 ? (
          <div className="panel p-6 text-sm text-slate-600">No games matched your criteria.</div>
        ) : (
          filtered.map((game) => (
            <article key={game.id} className="panel p-4 sm:p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs text-slate-500">{game.genre}</p>
                  <h2 className="mt-1 text-lg font-semibold text-slate-900">{game.title}</h2>
                  <p className="mt-2 text-sm text-slate-600">{game.shortDescription}</p>
                </div>
                <p className="text-sm font-semibold text-slate-700">{game.priceLabel}</p>
              </div>
              <div className="mt-4 flex items-center justify-between">
                <p className="text-xs text-slate-500">
                  Rating {game.rating} / Popularity {game.popularity}
                </p>
                <Link
                  href={`/detail/${game.id}`}
                  className="rounded-lg bg-teal-700 px-3 py-2 text-xs font-semibold text-white"
                >
                  Detail
                </Link>
              </div>
            </article>
          ))
        )}
      </section>
    </main>
  );
}
