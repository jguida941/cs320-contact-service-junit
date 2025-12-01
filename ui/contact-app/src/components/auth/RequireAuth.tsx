import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { authApi } from '@/lib/api';

/**
 * Route guard that requires a valid authentication token.
 *
 * <p>If the user is not authenticated the guard redirects to /login and
 * preserves the attempted location so the login page can bounce them back.
 */
export function RequireAuth() {
  const location = useLocation();

  if (!authApi.isAuthenticated()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}

/**
 * Route guard for pages that should only be visible when logged out.
 *
 * <p>Authenticated users hitting /login are redirected to the dashboard to
 * avoid showing a flickering login screen.
 */
export function PublicOnlyRoute() {
  if (authApi.isAuthenticated()) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
