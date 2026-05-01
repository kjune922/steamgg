"use client";

import { useSearchParams } from "next/navigation";

export default function NavSearch() {
  const searchParams = useSearchParams();
  const q = searchParams.get("q") ?? "";

  return (
    <input
      name="q"
      defaultValue={q}
      key={q}
      placeholder="게임 검색..."
      aria-label="게임 검색"
      className="h-9 min-w-0 flex-1 rounded-lg border border-[#2A313C] bg-[#181C22] px-3 text-sm text-[#F5F7FA] placeholder:text-[#8A93A0] focus:border-[#E60023] focus:outline-none focus:ring-1 focus:ring-[#E60023] transition-colors"
    />
  );
}
