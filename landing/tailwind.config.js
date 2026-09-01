/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        honey: {
          50: '#FFF8F0',
          100: '#FFEFD9',
          200: '#FFDFB3',
          300: '#FFCF8D',
          400: '#FFB84D',
          500: '#FF8C42',
          600: '#E67A3B',
          700: '#CC6834',
          800: '#B3562D',
          900: '#994426',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
