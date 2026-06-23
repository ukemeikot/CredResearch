import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        cosmos: {
          950: "#060916",
          900: "#0a0e27",
          800: "#11173a",
          700: "#1a2150",
          600: "#27306e",
        },
        accent: {
          DEFAULT: "#2de2ff",
          soft: "#7debff",
          deep: "#0fb5d9",
        },
        haze: "#7c3aed",
      },
      fontFamily: {
        sans: ["var(--font-inter)", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "var(--font-inter)", "sans-serif"],
      },
      boxShadow: {
        glow: "0 0 24px -2px rgba(45,226,255,0.45)",
        "glow-lg": "0 0 60px -4px rgba(45,226,255,0.55)",
        card: "0 20px 60px -20px rgba(0,0,0,0.6)",
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
          "0%,100%": { boxShadow: "0 0 24px -4px rgba(45,226,255,0.35)" },
          "50%": { boxShadow: "0 0 48px 2px rgba(45,226,255,0.65)" },
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
