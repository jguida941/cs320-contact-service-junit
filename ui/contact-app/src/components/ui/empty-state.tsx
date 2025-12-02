import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type EmptyStateActionConfig = {
  label: string;
  onClick: () => void;
};

interface EmptyStateProps {
  /**
   * Icon to display in the empty state
   */
  icon: LucideIcon;
  /**
   * Title text for the empty state
   */
  title: string;
  /**
   * Description text explaining the empty state
   */
  description: string;
  /**
   * Optional action button configuration or ReactNode
   */
  action?: ReactNode | EmptyStateActionConfig;
  /**
   * Additional CSS classes
   */
  className?: string;
}

/**
 * EmptyState component displays a message when no data is available.
 * Includes an icon, title, description, and optional action button.
 *
 * @example
 * ```tsx
 * <EmptyState
 *   icon={Users}
 *   title="No contacts yet"
 *   description="Get started by adding your first contact."
 *   action={{
 *     label: "Add Contact",
 *     onClick: handleAddContact
 *   }}
 * />
 * ```
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  const renderAction = () => {
    if (!action) {
      return null;
    }
    if (
      typeof action === 'object' &&
      action !== null &&
      'label' in action &&
      'onClick' in action &&
      typeof (action as { label?: unknown; onClick?: unknown }).label ===
        'string' &&
      typeof (action as { label?: unknown; onClick?: unknown }).onClick ===
        'function'
    ) {
      return (
        <Button onClick={action.onClick}>
          {action.label}
        </Button>
      );
    }
    return <>{action}</>;
  };

  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-8 text-center',
        className
      )}
    >
      <div className="rounded-full bg-muted p-3 mb-4">
        <Icon className="h-6 w-6 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">{title}</h3>
      <p className="text-sm text-muted-foreground mb-4 max-w-sm">
        {description}
      </p>
      {renderAction()}
    </div>
  );
}
