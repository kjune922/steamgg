import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import {
  fetchGame,
  fetchSimilarGames,
  formatRating,
  formatReviewScoreDescription,
  formatReviewSummary,
  hasSteamReviews,
} from "../../lib/games";

type DetailPageProps = {
  params: Promise<{ id: string }>;
};

export async function generateMetadata({ params }: DetailPageProps): Promise<Metadata> {
  const { id } = await params;
  const game = await fetchGame(id).catch(() => null);
  if (!game) return { title: "Game Not Found | steamgg" };

  const openGraph: Metadata["openGraph"] = {
    title: game.title,
    description: game.shortDescription,
  };
  if (game.coverImageUrl.startsWith("http")) {
    openGraph.images = [{ url: game.coverImageUrl }];
  }

  return {
    title: `${game.title} | steamgg`,
    description: game.shortDescription,
    openGraph,
  };
}

export default async function DetailPage({ params }: DetailPageProps) {
  const { id } = await params;
  const game = await fetchGame(id);

  if (!game) notFound();

  const similarGames = await fetchSimilarGames(id, 3);
  const reviewDescription = hasSteamReviews(game)
    ? formatReviewScoreDescription(game.reviewScoreDescription) || "Steam 리뷰"
    : "정보 없음";

  return (
    <main className="mx-auto w-full max-w-4xl px-5 py-8 sm:px-8 sm:py-10">
      {/* Back link */}
      <Link
        href="/list"
        className="inline-flex items-center gap-1 text-sm font-medium text-[#8A93A0] transition-colors hover:text-[#E60023]"
      >
        ← 목록으로
      </Link>

      {/* ── Hero image ── */}
      <div className="relative mt-4 h-56 w-full overflow-hidden rounded-xl sm:h-72 md:h-80">
        <Image
          src={game.coverImageUrl}
          alt={`${game.title} 커버 이미지`}
          fill
          priority
          sizes="(max-width: 896px) 100vw, 896px"
          className="object-cover"
        />
      </div>

      {/* ── Main content ── */}
      <div className="mt-4 panel p-5 sm:p-6">
        {/* Title + descriptions */}
        <h1 className="text-2xl font-bold text-[#F5F7FA] sm:text-3xl">
          {game.title}
        </h1>
        {game.shortDescription && (
          <p className="mt-2 text-sm font-medium text-[#8A93A0]">
            {game.shortDescription}
          </p>
        )}
        <p className="mt-3 text-sm leading-6 text-[#B6BEC9]">
          {game.description}
        </p>

        {/* ── CTA ── */}
        <div className="mt-5 flex flex-wrap gap-3">
          <a
            href={game.purchaseUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-xl bg-[#E60023] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#C4001E]"
          >
            구매하기 · {game.priceLabel}
          </a>
          <Link
            href={`/list?genre=${encodeURIComponent(game.genre)}`}
            className="inline-flex items-center rounded-xl border border-[#2A313C] bg-[#20252D] px-5 py-2.5 text-sm font-semibold text-[#B6BEC9] transition-colors hover:bg-[#2A313C]"
          >
            같은 장르 보기
          </Link>
        </div>

        {/* ── Metadata group ── */}
        <div className="mt-6 border-t border-[#2A313C] pt-5">
          <dl className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div>
              <dt className="text-xs text-[#8A93A0]">장르</dt>
              <dd className="mt-1 text-sm font-medium text-[#F5F7FA]">
                {game.genre}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-[#8A93A0]">SteamGG 점수</dt>
              <dd className="mt-1 text-sm font-medium text-[#F5F7FA]">
                {formatRating(game.rating)}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-[#8A93A0]">Steam 리뷰</dt>
              <dd className="mt-1 text-sm font-medium text-[#F5F7FA]">
                {reviewDescription}
              </dd>
              <dd className="mt-0.5 text-xs text-[#8A93A0]">
                {formatReviewSummary(game)}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-[#8A93A0]">인기 점수</dt>
              <dd className="mt-1 text-sm font-medium text-[#F5F7FA]">
                {game.popularity}
              </dd>
            </div>
          </dl>

          {/* Tags */}
          <div className="mt-4 flex flex-wrap gap-2">
            {game.tags.map((tag) => (
              <Link
                key={tag}
                href={`/list?tag=${encodeURIComponent(tag)}`}
                className="rounded-full border border-[#2A313C] bg-[#20252D] px-3 py-1 text-xs font-medium text-[#B6BEC9] transition-colors hover:border-[#E60023] hover:text-[#E60023]"
              >
                {tag}
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* ── Similar games ── */}
      {similarGames.length > 0 && (
        <section className="mt-8">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-base font-semibold text-[#F5F7FA]">
              비슷한 게임
            </h2>
            <Link
              href={`/list?genre=${encodeURIComponent(game.genre)}`}
              className="text-xs font-medium text-[#E60023] transition-colors hover:text-[#C4001E]"
            >
              더 보기 →
            </Link>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {similarGames.map((similar) => (
              <Link
                key={similar.id}
                href={`/detail/${similar.id}`}
                prefetch={false}
                className="panel panel-hover flex flex-col overflow-hidden"
              >
                <div className="relative h-36 w-full flex-shrink-0">
                  <Image
                    src={similar.coverImageUrl}
                    alt={`${similar.title} 커버 이미지`}
                    fill
                    sizes="(max-width: 640px) 100vw, 33vw"
                    className="object-cover"
                  />
                </div>
                <div className="flex flex-1 flex-col p-3">
                  <div className="flex items-center justify-between gap-1">
                    <span className="text-xs text-[#8A93A0]">{similar.genre}</span>
                    <span className="text-xs text-[#8A93A0]">★ {formatRating(similar.rating)}</span>
                  </div>
                  <h3 className="mt-1 line-clamp-1 text-sm font-semibold text-[#F5F7FA]">
                    {similar.title}
                  </h3>
                  <p className="mt-1 line-clamp-1 text-xs text-[#8A93A0]">
                    {similar.tags.slice(0, 2).join(" · ")}
                  </p>
                  <div className="mt-auto flex items-center justify-between pt-3">
                    <span className="min-w-0 truncate text-xs text-[#8A93A0]">
                      {formatReviewSummary(similar)}
                    </span>
                    <span className="text-xs font-semibold text-[#F5F7FA]">
                      {similar.priceLabel}
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}
