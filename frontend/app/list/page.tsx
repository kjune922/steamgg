import Image from "next/image";
import Link from "next/link";
import { fetchGames, genres } from "../lib/games";

type ListPageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

const PAGE_SIZE = 12;

function asValue(input: string | string[] | undefined) {
  if (Array.isArray(input)) return input[0] ?? "";
  return input ?? "";
}

const sortOptions = ["popular", "rating", "title"] as const;

function buildUrl(p: {
  q: string;
  genre: string;
  tag: string;
  sort: string;
  page?: number;
}) {
  const params = new URLSearchParams();
  if (p.q) params.set("q", p.q);
  if (p.genre && p.genre !== "All") params.set("genre", p.genre);
  if (p.tag) params.set("tag", p.tag);
  if (p.sort && p.sort !== "popular") params.set("sort", p.sort);
  if (p.page && p.page > 1) params.set("page", String(p.page));
  const qs = params.toString();
  return `/list${qs ? `?${qs}` : ""}`;
}

export default async function ListPage({ searchParams }: ListPageProps) {
  const raw = (await searchParams) ?? {};
  const rawQuery = asValue(raw.q).trim();
  const query = rawQuery.toLowerCase();
  const genreParam = asValue(raw.genre);
  const tagParam = asValue(raw.tag);
  const sortParam = asValue(raw.sort);
  const pageParam = parseInt(asValue(raw.page) || "1", 10);
  const currentPage = isNaN(pageParam) || pageParam < 1 ? 1 : pageParam;

  const genre = genres.includes(genreParam as (typeof genres)[number])
    ? genreParam
    : "All";
  const sort = sortOptions.includes(sortParam as (typeof sortOptions)[number])
    ? sortParam
    : "popular";

  const games = await fetchGames();
  const allTags = Array.from(new Set(games.flatMap((g) => g.tags))).sort();
  const tag = allTags.includes(tagParam) ? tagParam : "";

  let filtered = games.filter((game) => {
    const matchesQuery =
      query.length === 0 ||
      game.title.toLowerCase().includes(query) ||
      game.genre.toLowerCase().includes(query) ||
      game.shortDescription.toLowerCase().includes(query) ||
      game.tags.some((tag) => tag.toLowerCase().includes(query));
    const matchesGenre = genre === "All" || game.genre === genre;
    const matchesTag = tag === "" || game.tags.includes(tag);
    return matchesQuery && matchesGenre && matchesTag;
  });

  filtered = filtered.sort((a, b) => {
    if (sort === "rating") return b.rating - a.rating;
    if (sort === "title") return a.title.localeCompare(b.title);
    return b.popularity - a.popularity;
  });

  const totalCount = filtered.length;
  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages);
  const paginated = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  const sortLabels: Record<string, string> = {
    popular: "Popular",
    rating: "Rating",
    title: "A–Z",
  };

  const filterBase = { q: rawQuery, genre, tag, sort };

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10">
      {/* ── Search ── */}
      <form className="flex gap-2">
        {genre !== "All" && <input type="hidden" name="genre" value={genre} />}
        {tag && <input type="hidden" name="tag" value={tag} />}
        {sort !== "popular" && <input type="hidden" name="sort" value={sort} />}
        <input
          name="q"
          defaultValue={rawQuery}
          placeholder="Search games..."
          aria-label="Search games"
          className="h-11 flex-1 rounded-xl border border-[#2A313C] bg-[#181C22] px-4 text-sm text-[#F5F7FA] placeholder:text-[#8A93A0] focus:border-[#E60023] focus:outline-none focus:ring-1 focus:ring-[#E60023] transition-colors"
        />
        <button
          type="submit"
          className="h-11 cursor-pointer rounded-xl bg-[#E60023] px-5 text-sm font-semibold text-white transition-colors hover:bg-[#C4001E]"
        >
          Search
        </button>
      </form>

      {/* ── Filters ── */}
      <div className="mt-5 space-y-3">
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 text-xs font-medium text-[#8A93A0]">Genre</span>
          {genres.map((g) => (
            <Link
              key={g}
              href={buildUrl({ ...filterBase, genre: g })}
              className={`cursor-pointer rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                genre === g
                  ? "bg-[#E60023] text-white"
                  : "border border-[#2A313C] bg-[#181C22] text-[#B6BEC9] hover:bg-[#20252D]"
              }`}
            >
              {g}
            </Link>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 text-xs font-medium text-[#8A93A0]">Tag</span>
          {allTags.map((t) => {
            const isActive = tag === t;
            return (
              <Link
                key={t}
                href={buildUrl({ ...filterBase, tag: isActive ? "" : t })}
                className={`cursor-pointer rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                  isActive
                    ? "bg-[#2A1116] border border-[#E60023] text-[#E60023]"
                    : "border border-[#2A313C] bg-[#181C22] text-[#B6BEC9] hover:bg-[#20252D]"
                }`}
              >
                {t}
              </Link>
            );
          })}
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 text-xs font-medium text-[#8A93A0]">Sort</span>
          {sortOptions.map((s) => (
            <Link
              key={s}
              href={buildUrl({ ...filterBase, sort: s })}
              className={`cursor-pointer rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                sort === s
                  ? "bg-[#E60023] text-white"
                  : "border border-[#2A313C] bg-[#181C22] text-[#B6BEC9] hover:bg-[#20252D]"
              }`}
            >
              {sortLabels[s]}
            </Link>
          ))}
        </div>
      </div>

      {/* ── Active filters ── */}
      {(genre !== "All" || tag || sort !== "popular" || rawQuery) && (
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <span className="text-xs text-[#8A93A0]">Active:</span>
          {rawQuery && (
            <span className="rounded-md bg-[#20252D] px-2 py-1 text-xs text-[#B6BEC9]">
              "{rawQuery}"
            </span>
          )}
          {genre !== "All" && (
            <span className="rounded-md bg-[#20252D] px-2 py-1 text-xs text-[#B6BEC9]">
              {genre}
            </span>
          )}
          {tag && (
            <span className="rounded-md bg-[#20252D] px-2 py-1 text-xs text-[#B6BEC9]">
              #{tag}
            </span>
          )}
          <Link href="/list" className="ml-1 cursor-pointer text-xs text-[#8A93A0] underline hover:text-[#B6BEC9]">
            Clear all
          </Link>
        </div>
      )}

      {/* ── Results ── */}
      <section className="mt-6" aria-label="Search results">
        {paginated.length === 0 ? (
          <div className="panel flex flex-col items-center py-16 text-center">
            <p className="text-sm font-medium text-[#F5F7FA]">No games found</p>
            <p className="mt-1 text-xs text-[#8A93A0]">
              Try a different search term or remove a filter.
            </p>
            <Link
              href="/list"
              className="mt-4 cursor-pointer rounded-lg bg-[#E60023] px-4 py-2 text-xs font-semibold text-white hover:bg-[#C4001E]"
            >
              Clear filters
            </Link>
          </div>
        ) : (
          <>
            <p className="mb-4 text-xs text-[#8A93A0]">
              {totalCount}개 게임 · {safePage}/{totalPages} 페이지
            </p>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
              {paginated.map((game) => (
                <Link
                  key={game.id}
                  href={`/detail/${game.id}`}
                  className="panel panel-hover flex flex-col overflow-hidden"
                >
                  <div className="relative h-36 w-full flex-shrink-0 sm:h-40">
                    <Image
                      src={game.coverImageUrl}
                      alt={`${game.title} 커버 이미지`}
                      fill
                      sizes="(max-width: 768px) 50vw, 33vw"
                      className="object-cover"
                    />
                  </div>
                  <div className="flex flex-1 flex-col p-3">
                    <div className="flex items-center justify-between gap-1">
                      <span className="text-xs text-[#8A93A0]">{game.genre}</span>
                      <span className="text-xs text-[#8A93A0]">★ {game.rating}</span>
                    </div>
                    <h2 className="mt-1 line-clamp-1 text-sm font-semibold text-[#F5F7FA]">
                      {game.title}
                    </h2>
                    <p className="mt-1 line-clamp-1 text-xs text-[#8A93A0]">
                      {game.tags.slice(0, 2).join(" · ")}
                    </p>
                    <div className="mt-auto flex items-center justify-between pt-3">
                      <span className="text-xs text-[#8A93A0]">인기 {game.popularity}위</span>
                      <span className="text-xs font-semibold text-[#F5F7FA]">
                        {game.priceLabel}
                      </span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>

            {/* ── Pagination ── */}
            {totalPages > 1 && (
              <nav className="mt-8 flex items-center justify-center gap-2" aria-label="Pagination">
                <Link
                  href={buildUrl({ ...filterBase, page: safePage - 1 })}
                  aria-disabled={safePage <= 1}
                  className={`cursor-pointer rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
                    safePage <= 1
                      ? "pointer-events-none border-[#2A313C] text-[#4A525E]"
                      : "border-[#2A313C] bg-[#181C22] text-[#B6BEC9] hover:bg-[#20252D]"
                  }`}
                >
                  ← 이전
                </Link>

                <div className="flex gap-1">
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                    <Link
                      key={p}
                      href={buildUrl({ ...filterBase, page: p })}
                      className={`cursor-pointer min-w-[36px] rounded-lg border px-3 py-2 text-center text-sm font-medium transition-colors ${
                        p === safePage
                          ? "border-[#E60023] bg-[#E60023] text-white"
                          : "border-[#2A313C] bg-[#181C22] text-[#B6BEC9] hover:bg-[#20252D]"
                      }`}
                    >
                      {p}
                    </Link>
                  ))}
                </div>

                <Link
                  href={buildUrl({ ...filterBase, page: safePage + 1 })}
                  aria-disabled={safePage >= totalPages}
                  className={`cursor-pointer rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
                    safePage >= totalPages
                      ? "pointer-events-none border-[#2A313C] text-[#4A525E]"
                      : "border-[#2A313C] bg-[#181C22] text-[#B6BEC9] hover:bg-[#20252D]"
                  }`}
                >
                  다음 →
                </Link>
              </nav>
            )}
          </>
        )}
      </section>
    </main>
  );
}
