const SKELETON_SECTIONS = 3;
const SKELETON_CARDS = 5;

export default function Loading() {
  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-10 space-y-10">
      {Array.from({ length: SKELETON_SECTIONS }).map((_, si) => (
        <section key={si}>
          <div className="mb-3 flex items-center justify-between">
            <div className="h-4 w-24 animate-pulse rounded bg-[#2A313C]" />
            <div className="h-3 w-12 animate-pulse rounded bg-[#2A313C]" />
          </div>
          <div className="flex gap-3 overflow-hidden">
            {Array.from({ length: SKELETON_CARDS }).map((_, ci) => (
              <div
                key={ci}
                className="flex min-w-[160px] flex-col overflow-hidden rounded-xl border border-[#2A313C] bg-[#181C22] sm:min-w-[180px]"
              >
                <div className="h-24 w-full animate-pulse bg-[#2A313C] sm:h-28" />
                <div className="flex flex-col gap-2 p-3">
                  <div className="h-2.5 w-10 animate-pulse rounded bg-[#2A313C]" />
                  <div className="h-3.5 w-full animate-pulse rounded bg-[#2A313C]" />
                  <div className="mt-1 h-2.5 w-14 animate-pulse rounded bg-[#2A313C]" />
                </div>
              </div>
            ))}
          </div>
        </section>
      ))}
    </main>
  );
}
