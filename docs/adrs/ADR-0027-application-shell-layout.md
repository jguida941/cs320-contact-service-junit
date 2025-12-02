# ADR-0027: Application Shell Layout Pattern

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: [ADR-0017](ADR-0017-frontend-stack-and-application-shell.md), [ADR-0025](ADR-0025-ui-component-library.md)

## Context
- The contact app has three entity types (Contacts, Tasks, Appointments) requiring CRUD interfaces.
- Modern SaaS dashboards use consistent layout patterns: sidebar navigation, top bar, content area.
- Users expect responsive behavior: full layout on desktop, adapted layout on mobile.
- Create/edit workflows need a consistent pattern (modals vs drawers vs full pages).

## Decision
- Implement a **SaaS-style app shell** with:
  - **Collapsible left sidebar** (desktop), icons-only (tablet), bottom nav (mobile)
  - **Top bar** with page title, breadcrumb, global search trigger, profile/avatar
  - **Content area** that varies per route (cards, tables, lists)
  - **Right-hand drawer (sheet)** for create/edit operations (not centered modals)

### Layout Diagram (Desktop)
```
┌─────────────────────────────────────────────────────────────────┐
│ ┌──────────┐ ┌─────────────────────────────────────────────────┐│
│ │  Logo    │ │ TopBar: [Title] [Breadcrumb]  [🔍 Search] [👤] ││
│ ├──────────┤ └─────────────────────────────────────────────────┘│
│ │ Overview │ ┌──────────────────────────────────┬──────────────┐│
│ │ Contacts │ │                                  │              ││
│ │ Tasks    │ │          Content Area            │   Drawer     ││
│ │ Appts    │ │       (list/table/cards)         │   (details)  ││
│ │          │ │                                  │              ││
│ ├──────────┤ │                                  │              ││
│ │ Settings │ │                                  │              ││
│ │ Help     │ │                                  │              ││
│ └──────────┘ └──────────────────────────────────┴──────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Responsive Breakpoints
| Breakpoint          | Sidebar                 | Navigation            | Drawer            |
|---------------------|-------------------------|-----------------------|-------------------|
| Desktop (≥1024px)   | Full width with labels  | Left sidebar          | Right sheet       |
| Tablet (768-1023px) | Icons only, collapsible | Left sidebar (narrow) | Right sheet       |
| Mobile (<768px)     | Hidden                  | Bottom nav (4 icons)  | Full-screen sheet |

### Page Structure
- **Overview** (`/`): Dashboard with summary cards + recent activity feed
- **Entity pages** (`/contacts`, `/tasks`, `/appointments`):
  - Header: Title + "+ New" button + filter tabs
  - Content: Data table (desktop) or stacked cards (mobile)
  - Drawer: View/edit form on row click

### Drawer vs Modal Decision
| Pattern              | Use Case                 | Decision                                     |
|----------------------|--------------------------|----------------------------------------------|
| Right sheet (drawer) | Create/edit forms        | **Chosen** - feels modern, maintains context |
| Centered modal       | Quick confirmations only | Delete confirmations only                    |
| Full page            | Never for CRUD           | Rejected - loses list context                |

## Component Hierarchy
```
AppShell
├── Sidebar (desktop/tablet)
│   ├── Logo
│   ├── NavLinks (Overview, Contacts, Tasks, Appointments)
│   └── FooterLinks (Settings, Help)
├── MobileNav (mobile only)
│   └── BottomNavLinks (4 icons)
├── TopBar
│   ├── PageTitle + Breadcrumb
│   ├── SearchTrigger (opens CommandPalette)
│   └── ProfileAvatar
└── MainContent
    ├── <Outlet /> (React Router)
    └── EntityDrawer (Sheet component, context-controlled)
```

## Consequences
- Consistent navigation across all pages reduces cognitive load.
- Drawer pattern keeps users in context during CRUD operations.
- Mobile bottom nav follows iOS/Android native app conventions.
- Requires responsive CSS and `useMediaQuery` hook for breakpoint detection.
- Command palette (Ctrl+K) provides power-user quick navigation.

## Alternatives Considered
| Layout                   | Pros           | Cons                         | Decision |
|--------------------------|----------------|------------------------------|----------|
| Tab-based navigation     | Simple         | Doesn't scale to 4+ sections | Rejected |
| Top navbar only          | Common pattern | Less space for nav items     | Rejected |
| Full-page forms          | Simple routing | Loses list context           | Rejected |
| Centered modals for edit | Traditional    | Feels dated, blocks content  | Rejected |

## References
- [SaaS Dashboard Design Guide (Orbix Studio)](https://www.orbix.studio/blogs/saas-dashboard-design-b2b-optimization-guide)
- [Linear App](https://linear.app/) - Reference for sidebar + sheet pattern
- [shadcn/ui Sheet Component](https://ui.shadcn.com/docs/components/sheet)
