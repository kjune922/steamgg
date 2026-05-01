export default function Loading() {
  return (
    <main className="mx-auto w-full max-w-4xl px-5 py-8 sm:px-8 sm:py-10">
      {/* Back link */}
      <div className="h-4 w-16 animate-pulse rounded bg-[#2A313C]" />

      {/* Hero image */}
      <div className="mt-4 h-56 w-full animate-pulse rounded-xl bg-[#2A313C] sm:h-72 md:h-80" />

      {/* Main content */}
      <div className="mt-4 rounded-xl border border-[#2A313C] bg-[#181C22] p-5 sm:p-6">
        <div className="h-8 w-3/4 animate-pulse rounded bg-[#2A313C]" />
        <div className="mt-3 space-y-2">
          <div className="h-3.5 w-full animate-pulse rounded bg-[#2A313C]" />
          <div className="h-3.5 w-5/6 animate-pulse rounded bg-[#2A313C]" />
          <div className="h-3.5 w-4/6 animate-pulse rounded bg-[#2A313C]" />
        </div>
        <div className="mt-5 flex gap-3">
          <div className="h-10 w-36 animate-pulse rounded-xl bg-[#2A313C]" />
          <div className="h-10 w-28 animate-pulse rounded-xl bg-[#2A313C]" />
        </div>
        <div className="mt-6 border-t border-[#2A313C] pt-5">
          <div className="grid grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-2">
                <div className="h-2.5 w-8 animate-pulse rounded bg-[#2A313C]" />
                <div className="h-4 w-16 animate-pulse rounded bg-[#2A313C]" />
              </div>
            ))}
          </div>
          <div className="mt-4 flex gap-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-6 w-16 animate-pulse rounded-full bg-[#2A313C]" />
            ))}
          </div>
        </div>
      </div>

      {/* Similar games */}
      <div className="mt-8">
        <div className="mb-4 h-4 w-20 animate-pulse rounded bg-[#2A313C]" />
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="overflow-hidden rounded-xl border border-[#2A313C] bg-[#181C22]"
            >
              <div className="h-36 w-full animate-pulse bg-[#2A313C]" />
              <div className="flex flex-col gap-2 p-3">
                <div className="h-2.5 w-12 animate-pulse rounded bg-[#2A313C]" />
                <div className="h-4 w-full animate-pulse rounded bg-[#2A313C]" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}
