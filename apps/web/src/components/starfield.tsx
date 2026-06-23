"use client";

import { useEffect, useRef } from "react";

/** Lightweight animated starfield on a canvas — parallax twinkle + slow drift. */
export function Starfield() {
  const ref = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let raf = 0;
    let w = 0;
    let h = 0;
    const dpr = Math.min(window.devicePixelRatio || 1, 1.5);
    let stars: { x: number; y: number; r: number; a: number; tw: number; vx: number }[] = [];

    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    const resize = () => {
      w = canvas.clientWidth;
      h = canvas.clientHeight;
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const count = Math.min(110, Math.floor((w * h) / 15000));
      stars = Array.from({ length: count }, () => ({
        x: Math.random() * w,
        y: Math.random() * h,
        r: Math.random() * 1.4 + 0.2,
        a: Math.random(),
        tw: Math.random() * 0.02 + 0.004,
        vx: Math.random() * 0.06 + 0.01,
      }));
    };

    const draw = () => {
      ctx.clearRect(0, 0, w, h);
      for (const s of stars) {
        s.a += s.tw;
        const alpha = 0.35 + Math.abs(Math.sin(s.a)) * 0.65;
        if (!reduce) {
          s.x -= s.vx;
          if (s.x < -2) s.x = w + 2;
        }
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${s.r > 1 ? "150,220,255" : "255,255,255"},${alpha})`;
        ctx.fill();
      }
      raf = requestAnimationFrame(draw);
    };

    const onVisibility = () => {
      if (document.hidden) {
        cancelAnimationFrame(raf);
      } else {
        raf = requestAnimationFrame(draw);
      }
    };

    resize();
    draw();
    window.addEventListener("resize", resize);
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  return (
    <canvas
      ref={ref}
      aria-hidden
      className="pointer-events-none fixed inset-0 -z-10 h-full w-full opacity-70"
    />
  );
}
