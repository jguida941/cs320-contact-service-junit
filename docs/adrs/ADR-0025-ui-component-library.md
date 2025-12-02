# ADR-0025: UI Component Library Selection

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: [ADR-0017](ADR-0017-frontend-stack-and-application-shell.md), [REQUIREMENTS.md](../REQUIREMENTS.md), [Plan File](../../.claude/plans/robust-roaming-brooks.md)

## Context
- ADR-0017 established React + Vite + TypeScript but left "decisions on UI kit still open."
- The contact app needs forms, tables, drawers, modals, and accessible components for CRUD operations.
- Options evaluated: Material UI, Chakra UI, Mantine, Ant Design, shadcn/ui + Tailwind, plain CSS.
- Selection criteria: bundle size, customization flexibility, accessibility, developer experience, and alignment with modern SaaS dashboard patterns.

## Decision
- **Primary**: shadcn/ui with Tailwind CSS v4.
- **Rationale**:
  - **Copy-paste architecture**: Components are copied into the project, not installed as dependencies. Full control over source code, no vendor lock-in.
  - **Tailwind CSS v4**: Native CSS-first configuration via `@theme` directive. Design tokens become CSS variables automatically. Sub-40KB gzipped bundle.
  - **Accessibility**: Built on Radix UI primitives with ARIA attributes, keyboard navigation, and focus management out of the box.
  - **Theming**: CSS variable-based theming allows multiple professional themes (Slate, Ocean, Forest, Violet, Zinc) with light/dark variants.
  - **React 19 compatibility**: Works with React 19 and modern patterns (server components ready).
- **Supporting libraries**:
  - `@tanstack/react-query` for server state management
  - `@tanstack/react-table` for data tables
  - `react-hook-form` + `zod` for form validation (mirrors backend `Validation.java` rules)
  - `lucide-react` for icons
  - `date-fns` for date formatting

## Consequences
- Developers have full control over component styling and behavior.
- No breaking changes from upstream library updates since components are local.
- Requires Tailwind CSS knowledge; team must understand utility-first CSS.
- Theme switching requires CSS variable architecture (documented in ADR-0026).
- shadcn CLI (`npx shadcn@latest add <component>`) streamlines component installation.

## Alternatives Considered
| Library           | Pros                                 | Cons                                               | Decision                                             |
|-------------------|--------------------------------------|----------------------------------------------------|------------------------------------------------------|
| Material UI (MUI) | Enterprise-ready, 95k GitHub stars   | Large bundle (~200KB), opinionated Material Design | Rejected: too heavy, less customizable               |
| Chakra UI         | Excellent accessibility, style props | Medium bundle, less flexible theming               | Rejected: shadcn offers more control                 |
| Mantine           | Rich hooks, form library built-in    | Larger bundle, design less neutral                 | Rejected: form library overlaps with react-hook-form |
| Ant Design        | Enterprise-grade, rich components    | Very large bundle, distinct visual style           | Rejected: doesn't match modern SaaS aesthetic        |
| Plain CSS         | Zero dependencies                    | No component primitives, accessibility manual      | Rejected: too much effort for CRUD app               |

## References
- [shadcn/ui Documentation](https://ui.shadcn.com/)
- [Tailwind CSS v4 Theme Variables](https://tailwindcss.com/docs/theme)
- [Makers' Den: React UI Libraries 2025](https://makersden.io/blog/react-ui-libs-2025-comparing-shadcn-radix-mantine-mui-chakra)
- [GeeksforGeeks: React Architecture Patterns 2025](https://www.geeksforgeeks.org/reactjs/react-architecture-pattern-and-best-practices/)
