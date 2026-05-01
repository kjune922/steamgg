import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-col items-center justify-center px-5 py-24 text-center">
      <p className="text-6xl font-bold text-[#2A313C]">404</p>
      <p className="mt-4 text-sm font-medium text-[#F5F7FA]">페이지를 찾을 수 없습니다</p>
      <p className="mt-1 text-xs text-[#8A93A0]">
        게임이 존재하지 않거나 주소가 잘못됐습니다.
      </p>
      <Link
        href="/"
        className="mt-6 cursor-pointer rounded-lg bg-[#E60023] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#C4001E]"
      >
        홈으로 돌아가기
      </Link>
    </main>
  );
}
