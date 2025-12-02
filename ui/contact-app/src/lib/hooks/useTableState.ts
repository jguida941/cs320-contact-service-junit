import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Sort configuration
 */
export interface SortConfig<T> {
  key: keyof T;
  direction: SortDirection;
}

/**
 * Configuration for useTableState hook
 */
export interface UseTableStateConfig {
  defaultItemsPerPage?: number;
}

/**
 * Custom hook for managing table state (search, pagination, sorting) with URL query params
 *
 * Features:
 * - Search query synced with URL (?search=value)
 * - Pagination synced with URL (?page=1)
 * - Sorting synced with URL (?sort=fieldName&order=asc)
 * - Bookmarkable state (users can share URLs)
 * - Browser back/forward navigation support
 *
 * @param config - Configuration object
 * @returns Object with state and handlers
 */
export function useTableState<T>(config: UseTableStateConfig = {}) {
  const { defaultItemsPerPage = 10 } = config;
  const [searchParams, setSearchParams] = useSearchParams();

  // Extract state from URL query params
  const searchQuery = searchParams.get('search') || '';
  const currentPage = parseInt(searchParams.get('page') || '1', 10);
  const sortKey = searchParams.get('sort') as keyof T | null;
  const sortDirection = (searchParams.get('order') || 'asc') as SortDirection;

  const sortConfig: SortConfig<T> | null = sortKey
    ? { key: sortKey, direction: sortDirection }
    : null;

  /**
   * Update search query and reset to page 1
   */
  const setSearch = useCallback(
    (search: string) => {
      setSearchParams((params) => {
        const newParams = new URLSearchParams(params);
        if (search) {
          newParams.set('search', search);
        } else {
          newParams.delete('search');
        }
        // Reset to page 1 when search changes
        newParams.set('page', '1');
        return newParams;
      });
    },
    [setSearchParams]
  );

  /**
   * Update current page
   */
  const setPage = useCallback(
    (page: number) => {
      setSearchParams((params) => {
        const newParams = new URLSearchParams(params);
        newParams.set('page', page.toString());
        return newParams;
      });
    },
    [setSearchParams]
  );

  /**
   * Update sort configuration
   * - If clicking same column, toggle direction
   * - If clicking different column, set to ascending
   */
  const setSort = useCallback(
    (key: keyof T) => {
      setSearchParams((params) => {
        const newParams = new URLSearchParams(params);
        const currentSortKey = params.get('sort');
        const currentSortOrder = params.get('order') || 'asc';

        if (currentSortKey === String(key)) {
          // Toggle direction if same column
          newParams.set('order', currentSortOrder === 'asc' ? 'desc' : 'asc');
        } else {
          // Set new column with ascending order
          newParams.set('sort', String(key));
          newParams.set('order', 'asc');
        }

        // Reset to page 1 when sort changes
        newParams.set('page', '1');
        return newParams;
      });
    },
    [setSearchParams]
  );

  /**
   * Clear all filters and sorting
   */
  const clearFilters = useCallback(() => {
    setSearchParams({});
  }, [setSearchParams]);

  return {
    // State
    searchQuery,
    currentPage,
    sortConfig,
    itemsPerPage: defaultItemsPerPage,

    // Handlers
    setSearch,
    setPage,
    setSort,
    clearFilters,
  };
}

/**
 * Filter items based on search query
 *
 * @param items - Array of items to filter
 * @param searchQuery - Search query string
 * @param searchFields - Fields to search in (will convert to string and search)
 * @returns Filtered array
 */
export function filterItems<T>(
  items: T[],
  searchQuery: string,
  searchFields: (keyof T)[]
): T[] {
  if (!searchQuery.trim()) {
    return items;
  }

  const query = searchQuery.toLowerCase().trim();

  return items.filter((item) =>
    searchFields.some((field) => {
      const value = item[field];
      if (value == null) return false;
      return String(value).toLowerCase().includes(query);
    })
  );
}

/**
 * Sort items based on sort configuration
 *
 * @param items - Array of items to sort
 * @param sortConfig - Sort configuration (key and direction)
 * @returns Sorted array
 */
export function sortItems<T>(items: T[], sortConfig: SortConfig<T> | null): T[] {
  if (!sortConfig) {
    return items;
  }

  const { key, direction } = sortConfig;

  return [...items].sort((a, b) => {
    const aValue = a[key];
    const bValue = b[key];

    // Handle null/undefined values
    if (aValue == null && bValue == null) return 0;
    if (aValue == null) return 1;
    if (bValue == null) return -1;

    // Compare values
    let comparison = 0;
    if (aValue < bValue) {
      comparison = -1;
    } else if (aValue > bValue) {
      comparison = 1;
    }

    return direction === 'asc' ? comparison : -comparison;
  });
}

/**
 * Paginate items
 *
 * @param items - Array of items to paginate
 * @param currentPage - Current page number (1-indexed)
 * @param itemsPerPage - Number of items per page
 * @returns Object with paginated items and metadata
 */
export function paginateItems<T>(
  items: T[],
  currentPage: number,
  itemsPerPage: number
) {
  const totalItems = items.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));

  // Ensure current page is within bounds
  const safePage = Math.max(1, Math.min(currentPage, totalPages));

  const startIndex = (safePage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedItems = items.slice(startIndex, endIndex);

  return {
    items: paginatedItems,
    totalItems,
    totalPages,
    currentPage: safePage,
    itemsPerPage,
  };
}

/**
 * Combined hook that applies filtering, sorting, and pagination
 *
 * @param items - Original array of items
 * @param searchFields - Fields to search in
 * @returns Object with processed items and table state
 */
export function useFilteredSortedPaginatedData<T>(
  items: T[],
  searchFields: (keyof T)[]
) {
  const tableState = useTableState<T>();
  const { searchQuery, currentPage, sortConfig, itemsPerPage } = tableState;

  // Apply filtering, sorting, and pagination
  const processedData = useMemo(() => {
    // 1. Filter by search query
    const filtered = filterItems(items, searchQuery, searchFields);

    // 2. Sort
    const sorted = sortItems(filtered, sortConfig);

    // 3. Paginate
    const paginated = paginateItems(sorted, currentPage, itemsPerPage);

    return paginated;
  }, [items, searchQuery, searchFields, sortConfig, currentPage, itemsPerPage]);

  return {
    ...processedData,
    ...tableState,
  };
}
