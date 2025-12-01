import { QueryClient } from '@tanstack/react-query';

/**
 * Shared React Query client instance.
 * Exported separately to allow api.ts to clear the cache on logout
 * without creating circular dependencies with App.tsx.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60, // 1 minute
      retry: 1,
    },
  },
});
