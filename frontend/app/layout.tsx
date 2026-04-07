import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
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
  title: "steamgg MVP",
  description: "Game discovery MVP web",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <header className="sticky top-0 z-10 border-b border-[#2A313C] bg-[#0F1115]">
          <div className="mx-auto flex max-w-5xl items-center justify-between px-5 py-3 sm:px-8">
            <Link href="/" className="text-sm font-bold tracking-tight text-[#F5F7FA]">
              steamgg
            </Link>
            <button
              type="button"
              className="rounded-lg border border-[#2A313C] bg-[#181C22] px-4 py-2 text-sm font-semibold text-[#B6BEC9] transition-colors hover:bg-[#20252D]"
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
