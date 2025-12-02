import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { SkipLink } from './SkipLink';
import { useMediaQuery } from '@/hooks/useMediaQuery';

const pageTitles: Record<string, string> = {
  '/': 'Overview',
  '/contacts': 'Contacts',
  '/tasks': 'Tasks',
  '/appointments': 'Appointments',
  '/settings': 'Settings',
  '/help': 'Help',
};

export function AppShell() {
  const location = useLocation();
  const isDesktop = useMediaQuery('(min-width: 1024px)');
  const isTablet = useMediaQuery('(min-width: 768px)');
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const title = pageTitles[location.pathname] || 'ContactApp';

  // Desktop: full sidebar
  // Tablet: icons-only sidebar
  // Mobile: no sidebar (bottom nav would be added later)
  const showSidebar = isTablet;
  const sidebarCollapsed = !isDesktop;

  return (
    <>
      {/* A11y: Skip link for keyboard navigation - visible only on focus */}
      <SkipLink />

      <div className="flex h-screen overflow-hidden bg-background">
        {/* A11y: Navigation sidebar with proper semantic role */}
        {showSidebar && (
          <Sidebar collapsed={sidebarCollapsed && sidebarOpen} />
        )}

        {/* Main application container */}
        <div className="flex flex-1 flex-col overflow-hidden">
          {/* A11y: Header landmark with application title */}
          <TopBar
            title={title}
            showMenuButton={!isTablet}
            onMenuClick={() => setSidebarOpen(!sidebarOpen)}
          />

          {/* A11y: Main content landmark with id for skip link target */}
          <main
            id="main-content"
            className="flex-1 overflow-auto p-4 md:p-6"
            tabIndex={-1}
          >
            <Outlet />
          </main>
        </div>
      </div>
    </>
  );
}
