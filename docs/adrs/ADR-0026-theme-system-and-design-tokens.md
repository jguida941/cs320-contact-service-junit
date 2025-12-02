# ADR-0026: Theme System and Design Tokens

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: [ADR-0025](ADR-0025-ui-component-library.md), [Tailwind CSS Theme Docs](https://tailwindcss.com/docs/theme)

## Context
- Modern SaaS applications offer theme customization (light/dark mode, brand colors).
- The contact app targets a professional audience; multiple theme options demonstrate polish.
- Accessibility requirements mandate WCAG 2.1 AA contrast ratios (4.5:1 for text, 3:1 for UI elements).
- Tailwind CSS v4 introduces the `@theme` directive for CSS-first design tokens.

## Decision

- Implement a **CSS variable-based theme system** with 5 professional themes:
- 
  | Theme  | Feel                | Primary Color | Use Case              |
  |--------|---------------------|---------------|-----------------------|
  | Slate  | Clean, neutral      | Slate blue    | Default, professional |
  | Ocean  | Trust, stability    | Deep blue     | Fintech, B2B SaaS     |
  | Forest | Growth, balance     | Emerald green | Productivity          |
  | Violet | Modern, creative    | Purple        | Tech startups         |
  | Zinc   | Minimal, monochrome | Gray scale    | Developer tools       |

- Each theme has **light and dark variants** controlled via `.dark` class on `<html>`.
- **Token architecture**:
  ```
  :root → base CSS variables (--background, --primary, etc.)
  .theme-* → theme-specific overrides
  .dark.theme-* → dark mode overrides per theme
  @theme → Tailwind v4 semantic tokens that reference CSS variables
  ```
- **WCAG compliance**: All color pairs verified for 4.5:1+ contrast. Themes researched from real SaaS products (Stripe, Linear, Notion, Vercel, GitHub).

## Token Structure
```css
/* Semantic tokens (same names across all themes) */
--background      /* Page background */
--foreground      /* Primary text */
--card            /* Card/surface background */
--card-foreground /* Card text */
--primary         /* Primary actions/buttons */
--primary-foreground /* Text on primary */
--secondary       /* Secondary elements */
--muted           /* Subdued backgrounds */
--muted-foreground /* Subdued text */
--accent          /* Accent highlights */
--destructive     /* Error/danger actions */
--border          /* Borders */
--ring            /* Focus rings */
```

## Theme Switcher Implementation
```typescript
const themes = ['slate', 'ocean', 'forest', 'violet', 'zinc'] as const;

useEffect(() => {
  const root = document.documentElement;
  themes.forEach(t => root.classList.remove(`theme-${t}`));
  root.classList.add(`theme-${theme}`);
  darkMode ? root.classList.add('dark') : root.classList.remove('dark');
}, [theme, darkMode]);
```

## Consequences
- Users can switch themes at runtime without page reload.
- Dark mode support comes free with the architecture.
- Designers/developers can add new themes by defining CSS variables only.
- Must verify contrast ratios when adding themes (use WebAIM Contrast Checker).
- CSS variable approach has near-universal browser support (97%+ per caniuse).

## Alternatives Considered
| Approach                          | Pros                      | Cons                                | Decision |
|-----------------------------------|---------------------------|-------------------------------------|----------|
| CSS-in-JS (Styled Components)     | Scoped styles, JS theming | Runtime overhead, larger bundle     | Rejected |
| Tailwind `darkMode: 'class'` only | Simple light/dark         | No multi-theme support              | Rejected |
| CSS Modules                       | Scoped, no runtime        | Manual theming, verbose             | Rejected |
| Single theme only                 | Simplest                  | No customization, less professional | Rejected |

## References
- [shadcn/ui Themes](https://ui.shadcn.com/themes)
- [tweakcn Theme Editor](https://tweakcn.com/)
- [Tailwind CSS v4 @theme](https://tailwindcss.com/blog/tailwindcss-v4)
- [B2B SaaS Color Palette Guide](https://standardbeagle.com/accessible-color-palette/)
- [WCAG 2.1 Contrast Requirements](https://www.w3.org/WAI/WCAG21/quickref/#contrast-minimum)
