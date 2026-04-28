import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import { Suspense } from "react";
import NavSearch from "./components/NavSearch";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: { default: "steamgg", template: "%s | steamgg" },
  description: "게임을 못 정했을 때 찾아오는 곳. 장르, 태그, 분위기로 다음 게임을 탐색하세요.",
  openGraph: {
    siteName: "steamgg",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <header className="sticky top-0 z-10 border-b border-[#2A313C] bg-[#0F1115]">
          <div className="mx-auto flex max-w-5xl items-center gap-3 px-5 py-3 sm:px-8">
            <Link href="/" className="shrink-0 text-sm font-bold tracking-tight text-[#F5F7FA]">
              steamgg
            </Link>
            <form action="/list" className="flex flex-1 gap-2">
              <Suspense fallback={
                <input
                  placeholder="게임 검색..."
                  aria-label="게임 검색"
                  className="h-9 min-w-0 flex-1 rounded-lg border border-[#2A313C] bg-[#181C22] px-3 text-sm text-[#F5F7FA] placeholder:text-[#8A93A0]"
                  disabled
                />
              }>
                <NavSearch />
              </Suspense>
              <button
                type="submit"
                className="h-9 min-h-[44px] shrink-0 cursor-pointer rounded-lg bg-[#E60023] px-4 text-sm font-semibold text-white transition-colors hover:bg-[#C4001E] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#E60023]"
              >
                Search
              </button>
            </form>
            <button
              type="button"
              className="hidden min-h-[44px] shrink-0 cursor-pointer rounded-lg border border-[#2A313C] bg-[#181C22] px-4 py-2 text-sm font-semibold text-[#B6BEC9] transition-colors hover:bg-[#20252D] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#E60023] sm:block"
            >
              Login
            </button>
          </div>
        </header>
        {children}
      </body>
    </html>
  );
}
