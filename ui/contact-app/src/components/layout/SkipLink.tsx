/**
 * SkipLink Component
 *
 * WCAG 2.1 AA Compliance: Provides a "skip to main content" link that is
 * only visible when focused via keyboard navigation.
 *
 * This improves accessibility by allowing keyboard users to bypass repetitive
 * navigation elements and jump directly to the main content area.
 */

export function SkipLink() {
  return (
    <a
      href="#main-content"
      className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
    >
      Skip to main content
    </a>
  );
}
