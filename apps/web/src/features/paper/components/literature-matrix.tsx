"use client";

import { Grid3x3 } from "lucide-react";
import type { Paper } from "@/lib/api";
import { inTextCitation } from "../model/cite";

/**
 * Literature matrix (FR-LIT-5): a comparison table across the papers that have an AI summary —
 * reference, methodology, key findings, and identified gaps. Built client-side from the summaries
 * already stored on each paper (no extra fetch).
 */
export function LiteratureMatrix({ papers }: { papers: Paper[] }) {
  const rows = papers.filter((p) => p.summary && (p.summary.summary || p.summary.findings.length > 0));
  if (rows.length === 0) return null;

  return (
    <div className="mt-8">
      <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
        <Grid3x3 size={15} className="text-accent" /> Literature matrix
        <span className="text-xs font-normal text-slate-500">({rows.length} summarised)</span>
      </h4>
      <div className="mt-3 overflow-x-auto rounded-xl border border-slate-200">
        <table className="w-full min-w-[720px] border-collapse text-xs">
          <thead>
            <tr className="bg-white/[0.04] text-left text-slate-600">
              <th className="px-3 py-2 font-semibold">Reference</th>
              <th className="px-3 py-2 font-semibold">Methodology</th>
              <th className="px-3 py-2 font-semibold">Key findings</th>
              <th className="px-3 py-2 font-semibold">Research gaps</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id} className="border-t border-slate-100 align-top text-slate-500">
                <td className="px-3 py-2">
                  <span className="text-accent">{inTextCitation(p)}</span>
                  <div className="mt-0.5 line-clamp-2 text-slate-500">{p.title || p.filename}</div>
                </td>
                <td className="px-3 py-2">{p.summary?.methodology || "—"}</td>
                <td className="px-3 py-2">{list(p.summary?.findings)}</td>
                <td className="px-3 py-2">{list(p.summary?.gaps)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function list(items?: string[]) {
  if (!items || items.length === 0) return "—";
  return (
    <ul className="list-inside list-disc space-y-0.5">
      {items.map((it, i) => (
        <li key={i}>{it}</li>
      ))}
    </ul>
  );
}
