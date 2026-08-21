/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--tassist-bg)',
        'bg-elev': 'var(--tassist-bg-elev)',
        'bg-sunken': 'var(--tassist-bg-sunken)',
        border: 'var(--tassist-border)',
        'border-strong': 'var(--tassist-border-strong)',
        text: 'var(--tassist-text)',
        'text-muted': 'var(--tassist-text-muted)',
        'text-faint': 'var(--tassist-text-faint)',
        'text-inverse': 'var(--tassist-text-inverse)',
        primary: 'var(--tassist-primary)',
        'primary-hover': 'var(--tassist-primary-hover)',
        'primary-fg': 'var(--tassist-primary-fg)',
        'accent-peach': 'var(--tassist-accent-peach)',
        'accent-mint': 'var(--tassist-accent-mint)',
        'accent-amber': 'var(--tassist-accent-amber)',
        'accent-rose': 'var(--tassist-accent-rose)',
        success: 'var(--tassist-success)',
        warning: 'var(--tassist-warning)',
        danger: 'var(--tassist-danger)',
      },
      fontFamily: {
        display: ['Fraunces', 'Georgia', 'serif'],
        body: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      fontSize: {
        '2xs': 'var(--font-2xs)',
        xs: 'var(--font-xs)',
        sm: 'var(--font-sm)',
        md: 'var(--font-md)',
        lg: 'var(--font-lg)',
        xl: 'var(--font-xl)',
        '2xl': 'var(--font-2xl)',
        '3xl': 'var(--font-3xl)',
        '4xl': 'var(--font-4xl)',
      },
      borderRadius: {
        sm: 'var(--r-sm)',
        md: 'var(--r-md)',
        lg: 'var(--r-lg)',
        xl: 'var(--r-xl)',
        round: 'var(--r-round)',
      },
      boxShadow: {
        1: 'var(--shadow-1)',
        2: 'var(--shadow-2)',
      },
      spacing: {
        // spec scale (px): 4,8,12,16,24,32,48,64,96
        1: '4px', 2: '8px', 3: '12px', 4: '16px',
        6: '24px', 8: '32px', 12: '48px', 16: '64px', 24: '96px',
      },
    },
  },
  plugins: [],
}
