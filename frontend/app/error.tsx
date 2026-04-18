"use client";

import { useEffect } from "react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="mx-auto flex w-full max-w-5xl flex-col items-center justify-center px-5 py-24 text-center">
      <p className="text-sm font-medium text-[#F5F7FA]">문제가 발생했습니다</p>
      <p className="mt-1 text-xs text-[#8A93A0]">{error.message ?? "잠시 후 다시 시도해주세요."}</p>
      <button
        onClick={reset}
        className="mt-4 cursor-pointer rounded-lg bg-[#E60023] px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-[#C4001E]"
      >
        다시 시도
      </button>
    </main>
  );
}
