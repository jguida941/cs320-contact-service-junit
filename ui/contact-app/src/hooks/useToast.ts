import { toast } from 'sonner';
import type { ApiError } from '@/lib/api';

/**
 * Custom hook for showing toast notifications.
 * Provides convenient methods for success, error, and info toasts.
 */
export function useToast() {
  /**
   * Show a success toast notification.
   * @param message - The success message to display
   */
  const success = (message: string) => {
    toast.success(message);
  };

  /**
   * Show an error toast notification.
   * Handles both string messages and ApiError objects.
   * @param error - The error message or ApiError object
   */
  const error = (error: unknown) => {
    if (typeof error === 'string') {
      toast.error(error);
    } else if (error && typeof error === 'object' && 'message' in error) {
      const apiError = error as ApiError;
      toast.error(apiError.message);
    } else {
      toast.error('An unexpected error occurred');
    }
  };

  /**
   * Show an info toast notification.
   * @param message - The info message to display
   */
  const info = (message: string) => {
    toast.info(message);
  };

  /**
   * Show a warning toast notification.
   * @param message - The warning message to display
   */
  const warning = (message: string) => {
    toast.warning(message);
  };

  return {
    success,
    error,
    info,
    warning,
  };
}
