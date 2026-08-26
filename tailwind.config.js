/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        marca: {
          50: '#eff8ff',
          100: '#d9efff',
          200: '#b9e3ff',
          300: '#8bceff',
          400: '#55b7f5',
          500: '#2d8fd5',
          600: '#2374b5',
          700: '#1d5a8c',
          800: '#18466d',
          900: '#123653'
        }
      }
    }
  },
  plugins: []
};
