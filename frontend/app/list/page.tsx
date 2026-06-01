import Image from "next/image";
import Link from "next/link";
import { fetchGameFacets, fetchGamePage, formatRating, formatReviewSummary } from "../lib/games";

type ListPageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

const PAGE_SIZE = 12;
const VISIBLE_TAG_LIMIT = 18;

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

function includeSelected(options: string[], selected: string, fallbackPrefix: string[] = []) {
  const base = Array.from(new Set([...fallbackPrefix, ...options]));
  if (selected && selected !== "All" && !base.includes(selected)) {
    return [...fallbackPrefix, selected, ...base.filter((option) => !fallbackPrefix.includes(option))];
  }
  return base;
}

function splitTagOptions(options: string[], selected: string) {
  const visible = options.slice(0, VISIBLE_TAG_LIMIT);
  if (selected && !visible.includes(selected)) {
    visible.unshift(selected);
  }

  const visibleTags = Array.from(new Set(visible));
  return {
    visibleTags,
    hiddenTags: options.filter((option) => !visibleTags.includes(option)),
  };
}

function paginationItems(currentPage: number, totalPages: number): Array<number | "ellipsis"> {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages = new Set([1, totalPages, currentPage, currentPage - 1, currentPage + 1]);
  if (currentPage <= 4) {
    [2, 3, 4, 5].forEach((page) => pages.add(page));
  }
  if (currentPage >= totalPages - 3) {
    [totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1].forEach((page) => pages.add(page));
  }

  const sortedPages = Array.from(pages)
    .filter((page) => page >= 1 && page <= totalPages)
    .sort((a, b) => a - b);

  return sortedPages.flatMap((page, index) => {
    const previous = sortedPages[index - 1];
    if (!previous || page - previous === 1) {
      return [page];
    }
    if (page - previous === 2) {
      return [previous + 1, page];
    }
    return ["ellipsis" as const, page];
  });
}

export default async function ListPage({ searchParams }: ListPageProps) {
  const raw = (await searchParams) ?? {};
  const rawQuery = asValue(raw.q).trim();
  const genreParam = asValue(raw.genre);
  const tagParam = asValue(raw.tag);
  const sortParam = asValue(raw.sort);
  const pageParam = parseInt(asValue(raw.page) || "1", 10);
  const currentPage = isNaN(pageParam) || pageParam < 1 ? 1 : pageParam;

  const sort = sortOptions.includes(sortParam as (typeof sortOptions)[number])
    ? sortParam
    : "popular";

  const genre = genreParam || "All";
  const tag = tagParam;
  let gamePage: Awaited<ReturnType<typeof fetchGamePage>>;
  let facets: Awaited<ReturnType<typeof fetchGameFacets>>;
  try {
    [gamePage, facets] = await Promise.all([
      fetchGamePage({
        q: rawQuery,
        genre,
        tag,
        sort,
        page: currentPage,
        size: PAGE_SIZE,
      }),
      fetchGameFacets(),
    ]);
  } catch {
    return (
      <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10">
        <div className="panel flex flex-col items-center px-5 py-16 text-center">
          <p className="text-sm font-medium text-[#F5F7FA]">서버에 연결할 수 없습니다</p>
          <p className="mt-1 max-w-md text-xs text-[#8A93A0]">
            백엔드 API 상태를 확인한 뒤 검색과 필터를 다시 시도해주세요.
          </p>
          <Link
            href="/"
            className="mt-4 cursor-pointer rounded-lg border border-[#2A313C] bg-[#181C22] px-4 py-2 text-xs font-semibold text-[#B6BEC9] hover:bg-[#20252D]"
          >
            홈으로
          </Link>
        </div>
      </main>
    );
  }

  const paginated = gamePage.items;
  const totalCount = gamePage.totalCount;
  const totalPages = gamePage.totalPages;
  const safePage = gamePage.page;
  const genres = includeSelected(facets.genres, genre, ["All"]);
  const allTags = includeSelected(facets.tags, tag);
  const { visibleTags, hiddenTags } = splitTagOptions(allTags, tag);
  const pages = paginationItems(safePage, totalPages);

  const sortLabels: Record<string, string> = {
    popular: "Popular",
    rating: "Rating",
    title: "A–Z",
  };

  const filterBase = { q: rawQuery, genre, tag, sort };
  const hasActiveFilters = genre !== "All" || tag || sort !== "popular" || rawQuery;
  const activeFilterLabels = [
    rawQuery ? `"${rawQuery}"` : "",
    genre !== "All" ? genre : "",
    tag ? `#${tag}` : "",
    sort !== "popular" ? sortLabels[sort] : "",
  ].filter(Boolean);

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10">
      {/* ── Search ── */}
      <form className="flex flex-col gap-2 sm:flex-row">
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
          className="h-11 cursor-pointer rounded-xl bg-[#E60023] px-5 text-sm font-semibold text-white transition-colors hover:bg-[#C4001E] sm:w-auto"
        >
          Search
        </button>
      </form>

      {/* ── Filters ── */}
      <div className="mt-5 space-y-4 rounded-xl border border-[#2A313C] bg-[#101318] p-3 sm:p-4">
        <div className="flex items-center justify-between gap-3">
          <h1 className="text-sm font-semibold text-[#F5F7FA]">게임 목록</h1>
          {hasActiveFilters && (
            <Link href="/list" className="cursor-pointer text-xs font-medium text-[#8A93A0] underline hover:text-[#B6BEC9]">
              Clear all
            </Link>
          )}
        </div>

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

        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="mr-1 text-xs font-medium text-[#8A93A0]">Tag</span>
            {visibleTags.map((t) => {
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
          {hiddenTags.length > 0 && (
            <details className="group">
              <summary className="inline-flex min-h-[36px] cursor-pointer list-none items-center rounded-lg border border-[#2A313C] bg-[#181C22] px-3 py-1.5 text-xs font-medium text-[#B6BEC9] transition-colors hover:bg-[#20252D]">
                태그 더 보기 ({hiddenTags.length})
              </summary>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {hiddenTags.map((t) => {
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
            </details>
          )}
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
      {hasActiveFilters && (
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <span className="text-xs text-[#8A93A0]">Active:</span>
          {rawQuery && (
            <span className="rounded-md bg-[#20252D] px-2 py-1 text-xs text-[#B6BEC9]">
              &quot;{rawQuery}&quot;
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
        </div>
      )}

      {/* ── Results ── */}
      <section className="mt-6" aria-label="Search results">
        {paginated.length === 0 ? (
          <div className="panel flex flex-col items-center px-5 py-16 text-center">
            <p className="text-sm font-medium text-[#F5F7FA]">검색 결과가 없습니다</p>
            <p className="mt-1 max-w-md text-xs text-[#8A93A0]">
              {activeFilterLabels.length > 0
                ? `${activeFilterLabels.join(" · ")} 조건에 맞는 게임을 찾지 못했습니다.`
                : "현재 표시할 게임 데이터가 없습니다."}
            </p>
            <div className="mt-4 flex flex-wrap justify-center gap-2">
              <Link
                href="/list"
                className="cursor-pointer rounded-lg bg-[#E60023] px-4 py-2 text-xs font-semibold text-white hover:bg-[#C4001E]"
              >
                Clear filters
              </Link>
              <Link
                href="/"
                className="cursor-pointer rounded-lg border border-[#2A313C] bg-[#181C22] px-4 py-2 text-xs font-semibold text-[#B6BEC9] hover:bg-[#20252D]"
              >
                홈으로
              </Link>
            </div>
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
                      <span className="text-xs text-[#8A93A0]">★ {formatRating(game.rating)}</span>
                    </div>
                    <h2 className="mt-1 line-clamp-1 text-sm font-semibold text-[#F5F7FA]">
                      {game.title}
                    </h2>
                    <p className="mt-1 line-clamp-1 text-xs text-[#8A93A0]">
                      {game.tags.slice(0, 2).join(" · ")}
                    </p>
                    <div className="mt-auto flex items-center justify-between gap-2 pt-3">
                      <span className="min-w-0 truncate text-xs text-[#8A93A0]">
                        {formatReviewSummary(game)}
                      </span>
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
              <nav className="mt-8 flex flex-wrap items-center justify-center gap-2" aria-label="Pagination">
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

                <div className="flex flex-wrap justify-center gap-1">
                  {pages.map((p, index) => (
                    p === "ellipsis" ? (
                      <span
                        key={`ellipsis-${index}`}
                        className="min-w-[36px] px-2 py-2 text-center text-sm font-medium text-[#8A93A0]"
                      >
                        ...
                      </span>
                    ) : (
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
                    )
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
