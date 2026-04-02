import Link from "next/link";
import { findGameById, getSimilarGames } from "../../lib/games";

type DetailPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DetailPage({ params }: DetailPageProps) {
  const { id } = await params;
  const game = findGameById(id);

  if (!game) {
    return (
      <main className="mx-auto w-full max-w-4xl px-5 py-8 sm:px-8 sm:py-10">
        <section className="panel p-6">
          <h1 className="text-2xl font-bold text-slate-900">Game not found</h1>
          <Link href="/list" className="mt-4 inline-block text-sm font-semibold text-teal-700">
            Back to list
          </Link>
        </section>
      </main>
    );
  }

  const similarGames = getSimilarGames(game, 3);

  return (
    <main className="mx-auto w-full max-w-4xl px-5 py-8 sm:px-8 sm:py-10">
      <section className="panel p-5 sm:p-6">
        <img
          src={game.coverImageUrl}
          alt={`${game.title} cover`}
          className="h-44 w-full rounded-xl object-cover sm:h-56"
        />
        <p className="text-xs text-slate-500">{game.genre}</p>
        <h1 className="mt-1 text-3xl font-bold text-slate-900">{game.title}</h1>
        <p className="mt-3 text-sm leading-6 text-slate-700">{game.description}</p>
        <div className="mt-4 flex flex-wrap gap-2">
          {game.tags.map((tag) => (
            <span
              key={tag}
              className="rounded-full border border-slate-300 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700"
            >
              {tag}
            </span>
          ))}
        </div>

        <div className="mt-5 flex flex-wrap gap-3">
          <a
            href={game.purchaseUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white"
          >
            Purchase {game.priceLabel}
          </a>
          <Link
            href={`/list?q=${encodeURIComponent(game.title)}`}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800"
          >
            Find in list
          </Link>
        </div>

        <p className="mt-4 text-xs text-slate-500">
          Rating {game.rating} / Popularity {game.popularity}
        </p>
      </section>

      <section className="mt-6">
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Similar games</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {similarGames.map((similarGame) => (
            <Link key={similarGame.id} href={`/detail/${similarGame.id}`} className="panel p-4">
              <img
                src={similarGame.coverImageUrl}
                alt={`${similarGame.title} cover`}
                className="h-28 w-full rounded-lg object-cover"
              />
              <p className="text-xs text-slate-500">{similarGame.genre}</p>
              <h3 className="mt-1 text-base font-semibold text-slate-900">{similarGame.title}</h3>
              <p className="mt-2 text-sm text-slate-600">{similarGame.shortDescription}</p>
            </Link>
          ))}
        </div>
      </section>
    </main>
  );
}
