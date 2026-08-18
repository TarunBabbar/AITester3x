import type { Metadata } from "next";
import "./globals.css";
import Sidebar from "@/components/nav/CollapsibleSidebar";

export const metadata: Metadata = {
  title: "QA STLC Studio",
  description:
    "AI-powered Software Testing Life Cycle system — agents for requirements, planning, test cases, execution, triage, and reporting.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen">
        <div className="flex min-h-screen">
          <Sidebar />
          <main className="flex-1 p-8">{children}</main>
        </div>
      </body>
    </html>
  );
}
