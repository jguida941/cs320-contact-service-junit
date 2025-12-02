import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import { TableHead } from './table';
import { cn } from '@/lib/utils';
import type { SortConfig } from '@/lib/hooks/useTableState';

/**
 * SortableTableHead component for clickable column headers with sort indicators
 *
 * Features:
 * - Clickable column header
 * - Visual indicators for sort direction (arrows)
 * - Accessible labels
 * - Hover state
 *
 * @param children - Column header content
 * @param sortKey - The key to sort by when clicked
 * @param currentSort - Current sort configuration
 * @param onSort - Callback when header is clicked
 * @param className - Additional CSS classes
 */
interface SortableTableHeadProps<T> {
  children: React.ReactNode;
  sortKey: keyof T;
  currentSort: SortConfig<T> | null;
  onSort: (key: keyof T) => void;
  className?: string;
}

export function SortableTableHead<T>({
  children,
  sortKey,
  currentSort,
  onSort,
  className,
}: SortableTableHeadProps<T>) {
  const isSorted = currentSort?.key === sortKey;
  const direction = isSorted ? currentSort.direction : null;

  return (
    <TableHead className={cn(className)}>
      <button
        type="button"
        onClick={() => onSort(sortKey)}
        className="flex w-full items-center gap-2 font-medium hover:text-foreground"
      >
        <span>{children}</span>
        <span className="ml-auto">
          {direction === 'asc' && <ArrowUp className="h-4 w-4" />}
          {direction === 'desc' && <ArrowDown className="h-4 w-4" />}
          {!direction && <ArrowUpDown className="h-4 w-4 opacity-50" />}
        </span>
      </button>
    </TableHead>
  );
}
