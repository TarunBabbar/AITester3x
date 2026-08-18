import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: {
          primary: "#F5F0E8",
          surface: "#FBF8F3",
          sidebar: "#EDE6D8",
        },
        text: {
          primary: "#2B2A27",
          muted: "#6B6558",
        },
        accent: "#C96442",
        border: "#E2D9C7",
        success: "#4F7942",
        warning: "#C9A227",
        danger: "#B3452C",
      },
      borderRadius: {
        DEFAULT: "0.75rem",
      },
      fontFamily: {
        serif: ['"Tiempos"', "Georgia", "serif"],
        sans: ['"Inter"', "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
