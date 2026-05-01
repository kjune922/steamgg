const SKELETON_COUNT = 9;

export default function Loading() {
  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10">
      {/* Search bar skeleton */}
      <div className="flex gap-2">
        <div className="h-11 flex-1 animate-pulse rounded-xl bg-[#2A313C]" />
        <div className="h-11 w-20 animate-pulse rounded-xl bg-[#2A313C]" />
      </div>

      {/* Filter chips skeleton */}
      <div className="mt-5 space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="flex gap-2">
            <div className="h-7 w-10 animate-pulse rounded-lg bg-[#2A313C]" />
            {Array.from({ length: 4 }).map((_, j) => (
              <div key={j} className="h-7 w-16 animate-pulse rounded-lg bg-[#2A313C]" />
            ))}
          </div>
        ))}
      </div>

      {/* Results skeleton */}
      <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3">
        {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
          <div
            key={i}
            className="flex flex-col overflow-hidden rounded-xl border border-[#2A313C] bg-[#181C22]"
          >
            <div className="h-36 w-full animate-pulse bg-[#2A313C] sm:h-40" />
            <div className="flex flex-1 flex-col gap-2 p-3">
              <div className="h-2.5 w-12 animate-pulse rounded bg-[#2A313C]" />
              <div className="h-4 w-full animate-pulse rounded bg-[#2A313C]" />
              <div className="h-2.5 w-20 animate-pulse rounded bg-[#2A313C]" />
              <div className="mt-auto flex justify-between pt-2">
                <div className="h-2.5 w-12 animate-pulse rounded bg-[#2A313C]" />
                <div className="h-2.5 w-10 animate-pulse rounded bg-[#2A313C]" />
              </div>
            </div>
          </div>
        ))}
      </div>
    </main>
  );
}
