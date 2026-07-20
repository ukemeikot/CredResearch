import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Light "paper" surface scale. The app was authored against a dark `cosmos-*` scale;
        // these values re-tone those class names to a clean academic light theme.
        cosmos: {
          950: "#ffffff", // page background
          900: "#ffffff", // cards / menus / popovers
          800: "#f1f5f9",
          700: "#e2e8f0",
          600: "#cbd5e1",
        },
        // Academic, modern, student-friendly: indigo.
        accent: {
          DEFAULT: "#4f46e5",
          soft: "#6366f1",
          deep: "#4338ca",
        },
        haze: "#7c3aed",
      },
      fontFamily: {
        sans: ["var(--font-inter)", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "var(--font-inter)", "sans-serif"],
      },
      boxShadow: {
        // Soft elevation shadows for a light UI.
        glow: "0 6px 20px -6px rgba(79,70,229,0.35)",
        "glow-lg": "0 12px 34px -8px rgba(79,70,229,0.4)",
        card: "0 1px 2px rgba(15,23,42,0.06), 0 12px 32px -16px rgba(15,23,42,0.18)",
      },
      keyframes: {
        float: {
          "0%,100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-18px)" },
        },
        twinkle: {
          "0%,100%": { opacity: "0.3" },
          "50%": { opacity: "1" },
        },
        "pulse-glow": {
          "0%,100%": { boxShadow: "0 0 24px -4px rgba(79,70,229,0.18)" },
          "50%": { boxShadow: "0 0 44px 2px rgba(79,70,229,0.3)" },
        },
        "gradient-pan": {
          "0%,100%": { backgroundPosition: "0% 50%" },
          "50%": { backgroundPosition: "100% 50%" },
        },
        "spin-slow": {
          to: { transform: "rotate(360deg)" },
        },
      },
      animation: {
        float: "float 7s ease-in-out infinite",
        twinkle: "twinkle 3s ease-in-out infinite",
        "pulse-glow": "pulse-glow 4s ease-in-out infinite",
        "gradient-pan": "gradient-pan 8s ease infinite",
        "spin-slow": "spin-slow 40s linear infinite",
      },
    },
  },
  plugins: [],
};

export default config;
