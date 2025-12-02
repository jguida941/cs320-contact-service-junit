import { Navigate, Outlet } from 'react-router-dom';
import { authApi } from '@/lib/api';

/**
 * Route guard that requires ADMIN role.
 *
 * <p>If the user is not authenticated or does not have ADMIN role,
 * redirects to the home page (overview). This prevents unauthorized
 * access to admin-only routes.
 *
 * <p>Per ADR-0036, admin routes are protected both client-side (this guard)
 * and server-side (@PreAuthorize annotations) for defense in depth.
 */
export function RequireAdmin() {
  const user = authApi.getCurrentUser();

  // Redirect if not authenticated or not an admin
  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
