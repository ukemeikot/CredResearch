import type { Metadata } from "next";
import { Inter, Space_Grotesk } from "next/font/google";
import { Providers } from "@/components/providers";
import { Starfield } from "@/components/starfield";
import "./globals.css";

const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });
const display = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-display",
  display: "swap",
});

export const metadata: Metadata = {
  title: "CredResearch — Research Revolution",
  description:
    "AI-powered academic research workflow & supervision platform. From idea to a structured, supervisor-reviewed, citation-supported document — transparently.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${inter.variable} ${display.variable}`}>
      <body>
        <Starfield />
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
