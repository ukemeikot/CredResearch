"use client";

import { AnimatePresence, motion } from "framer-motion";
import { Eraser, Pen, Trash2, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Portal } from "@/components/ui/portal";
import { api } from "@/lib/api";

const WIDTH = 900;
const HEIGHT = 540;
const COLORS = ["#0f172a", "#4f46e5", "#dc2626", "#059669", "#d97706", "#2563eb"];

/** A lightweight freehand drawing pad. On save the canvas is rendered to a PNG, uploaded as a
 * document asset, and inserted into the editor as an image. */
export function SketchModal({
  docId,
  open,
  onClose,
  onInserted,
}: {
  docId: string;
  open: boolean;
  onClose: () => void;
  onInserted: (url: string) => void;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const drawing = useRef(false);
  const last = useRef<{ x: number; y: number } | null>(null);
  const [color, setColor] = useState(COLORS[0]);
  const [size, setSize] = useState(3);
  const [erasing, setErasing] = useState(false);
  const [saving, setSaving] = useState(false);

  // Reset to a clean white canvas each time it opens.
  useEffect(() => {
    if (!open) return;
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx) return;
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, WIDTH, HEIGHT);
    setErasing(false);
  }, [open]);

  function pos(e: React.PointerEvent<HTMLCanvasElement>) {
    const rect = canvasRef.current!.getBoundingClientRect();
    return {
      x: ((e.clientX - rect.left) / rect.width) * WIDTH,
      y: ((e.clientY - rect.top) / rect.height) * HEIGHT,
    };
  }

  function start(e: React.PointerEvent<HTMLCanvasElement>) {
    drawing.current = true;
    last.current = pos(e);
    canvasRef.current?.setPointerCapture(e.pointerId);
  }

  function move(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!drawing.current) return;
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx || !last.current) return;
    const p = pos(e);
    ctx.strokeStyle = erasing ? "#ffffff" : color;
    ctx.lineWidth = erasing ? size * 6 : size;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.beginPath();
    ctx.moveTo(last.current.x, last.current.y);
    ctx.lineTo(p.x, p.y);
    ctx.stroke();
    last.current = p;
  }

  function end() {
    drawing.current = false;
    last.current = null;
  }

  function clear() {
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx) return;
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, WIDTH, HEIGHT);
  }

  async function save() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    setSaving(true);
    try {
      const blob: Blob | null = await new Promise((res) => canvas.toBlob(res, "image/png"));
      if (!blob) return;
      const file = new File([blob], `drawing-${Date.now()}.png`, { type: "image/png" });
      const { url } = await api.uploadDocAsset(docId, file);
      onInserted(url);
      onClose();
    } catch {
      /* upload failed — keep the modal open so the drawing isn't lost */
    } finally {
      setSaving(false);
    }
  }

  return (
    <Portal>
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-50 grid place-items-center bg-slate-900/40 px-6 py-10 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.94, y: 16 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.96, y: 8 }}
              transition={{ type: "spring", stiffness: 320, damping: 26 }}
              onClick={(e) => e.stopPropagation()}
              className="glass w-full max-w-3xl rounded-2xl p-5 shadow-card"
            >
              <div className="flex items-center justify-between">
                <h2 className="font-display text-lg font-bold text-slate-900">Drawing</h2>
                <button onClick={onClose} className="text-slate-500 hover:text-slate-900" aria-label="Close">
                  <X size={18} />
                </button>
              </div>

              {/* Tools */}
              <div className="mt-4 flex flex-wrap items-center gap-2">
                {COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => {
                      setColor(c);
                      setErasing(false);
                    }}
                    className={`h-6 w-6 rounded-full border-2 transition-transform ${
                      color === c && !erasing ? "scale-110 border-slate-900" : "border-white"
                    }`}
                    style={{ backgroundColor: c }}
                    aria-label={`Colour ${c}`}
                  />
                ))}
                <span className="mx-1 w-px self-stretch bg-slate-200" />
                <button
                  type="button"
                  onClick={() => setErasing(false)}
                  className={`grid h-8 w-8 place-items-center rounded-lg border ${!erasing ? "border-accent/60 bg-accent/10 text-slate-900" : "border-slate-200 text-slate-600"}`}
                  aria-label="Pen"
                >
                  <Pen size={15} />
                </button>
                <button
                  type="button"
                  onClick={() => setErasing(true)}
                  className={`grid h-8 w-8 place-items-center rounded-lg border ${erasing ? "border-accent/60 bg-accent/10 text-slate-900" : "border-slate-200 text-slate-600"}`}
                  aria-label="Eraser"
                >
                  <Eraser size={15} />
                </button>
                <input
                  type="range"
                  min={1}
                  max={12}
                  value={size}
                  onChange={(e) => setSize(Number(e.target.value))}
                  className="ml-1 w-24 accent-indigo-500"
                  aria-label="Brush size"
                />
                <button
                  type="button"
                  onClick={clear}
                  className="ml-auto inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 hover:border-rose-300 hover:text-rose-600"
                >
                  <Trash2 size={14} /> Clear
                </button>
              </div>

              <div className="mt-3 overflow-hidden rounded-xl border border-slate-200">
                <canvas
                  ref={canvasRef}
                  width={WIDTH}
                  height={HEIGHT}
                  onPointerDown={start}
                  onPointerMove={move}
                  onPointerUp={end}
                  onPointerLeave={end}
                  className="block w-full touch-none bg-white"
                  style={{ aspectRatio: `${WIDTH} / ${HEIGHT}`, cursor: "crosshair" }}
                />
              </div>

              <div className="mt-4 flex justify-end gap-3">
                <Button type="button" variant="outline" onClick={onClose}>
                  Cancel
                </Button>
                <Button type="button" onClick={save} disabled={saving}>
                  {saving ? "Inserting…" : "Insert drawing"}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </Portal>
  );
}
