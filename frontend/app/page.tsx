import Image from "next/image";
import Link from "next/link";
import { fetchCurations, formatRating, formatReviewSummary, type Game } from "./lib/games";

export const dynamic = "force-dynamic";

export default async function Home() {
  let sections: Awaited<ReturnType<typeof fetchCurations>>;
  try {
    sections = await fetchCurations();
  } catch {
    return (
      <main className="mx-auto flex w-full max-w-5xl flex-col items-center justify-center px-5 py-24 text-center">
        <p className="text-sm font-medium text-[#F5F7FA]">서버에 연결할 수 없습니다</p>
        <p className="mt-1 text-xs text-[#8A93A0]">백엔드 API 상태를 확인한 뒤 다시 시도해주세요.</p>
      </main>
    );
  }

  if (sections.length === 0) {
    return (
      <main className="mx-auto flex w-full max-w-5xl flex-col items-center justify-center px-5 py-24 text-center">
        <p className="text-sm font-medium text-[#F5F7FA]">표시할 큐레이션이 없습니다</p>
        <p className="mt-1 text-xs text-[#8A93A0]">관리자 동기화 후 게임 데이터가 채워집니다.</p>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10 space-y-10">
      {sections.map((section) => (
        <CurationSection
          key={section.key}
          title={section.title}
          href={section.href}
          games={section.items}
        />
      ))}
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
  games: Game[];
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
              <div className="mt-auto pt-2">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-medium text-[#B6BEC9]">★ {formatRating(game.rating)}</span>
                  <span className="truncate text-xs font-semibold text-[#F5F7FA]">
                    {game.priceLabel}
                  </span>
                </div>
                <p className="mt-1 truncate text-xs text-[#8A93A0]">
                  {formatReviewSummary(game)}
                </p>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
