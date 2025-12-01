import type { Contact, ContactRequest, Task, TaskRequest, Appointment, AppointmentRequest } from './schemas';
import { queryClient } from './queryClient';

const API_BASE = '/api/v1';
const AUTH_BASE = '/api/auth';

// ==================== Auth Types ====================

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  role: 'USER' | 'ADMIN';
  expiresIn: number;
}

// ==================== Token Management ====================

const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';
const PROFILE_STORAGE_KEY = 'contact-app-profile';

export const tokenStorage = {
  getToken: (): string | null => localStorage.getItem(TOKEN_KEY),
  setToken: (token: string): void => localStorage.setItem(TOKEN_KEY, token),
  removeToken: (): void => localStorage.removeItem(TOKEN_KEY),

  getUser: (): AuthResponse | null => {
    const user = localStorage.getItem(USER_KEY);
    return user ? JSON.parse(user) : null;
  },
  setUser: (user: AuthResponse): void => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  removeUser: (): void => localStorage.removeItem(USER_KEY),

  clear: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(PROFILE_STORAGE_KEY);
  },
};

/**
 * Returns authorization headers if a token is available.
 */
function getAuthHeaders(): Record<string, string> {
  const token = tokenStorage.getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * Normalized API error with message and optional field errors.
 */
export interface ApiError {
  message: string;
  status: number;
  errors?: Record<string, string>;
}

/**
 * Handles fetch response, throwing ApiError on non-2xx status.
 * Returns parsed JSON for successful responses.
 * Automatically logs out and redirects on 401/403 errors.
 */
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // Handle authentication errors (invalid/expired token) with auto-logout
    if (response.status === 401) {
      tokenStorage.clear();
      queryClient.clear(); // Clear cached data to prevent data leakage
      // Redirect to login page if not already there
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }

    let message = `HTTP ${response.status}`;
    let errors: Record<string, string> | undefined;

    try {
      const body = await response.json();
      message = body.message || body.error || message;
      errors = body.errors;
    } catch {
      // Response body is not JSON, use status text
      message = response.statusText || message;
    }

    const error: ApiError = { message, status: response.status, errors };
    throw error;
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

// ==================== Auth API ====================

export const authApi = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await fetch(`${AUTH_BASE}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    const result = await handleResponse<AuthResponse>(response);
    tokenStorage.setToken(result.token);
    tokenStorage.setUser(result);
    syncProfileFromAuth(result);
    return result;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await fetch(`${AUTH_BASE}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    const result = await handleResponse<AuthResponse>(response);
    tokenStorage.setToken(result.token);
    tokenStorage.setUser(result);
    syncProfileFromAuth(result);
    return result;
  },

  logout: async (): Promise<void> => {
    // Notify backend of logout (for audit logging, future token blacklisting)
    try {
      const token = tokenStorage.getToken();
      if (token) {
        await fetch(`${AUTH_BASE}/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });
      }
    } catch {
      // Ignore errors - proceed with client-side cleanup regardless
    }
    tokenStorage.clear();
    queryClient.clear(); // Clear cached data to prevent data leakage to next user
  },

  isAuthenticated: (): boolean => {
    return tokenStorage.getToken() !== null;
  },

  getCurrentUser: (): AuthResponse | null => {
    return tokenStorage.getUser();
  },
};

function syncProfileFromAuth(response: AuthResponse) {
  const initials = computeInitials(response.username);
  localStorage.setItem(
    PROFILE_STORAGE_KEY,
    JSON.stringify({
      name: response.username,
      email: response.email,
      initials,
    })
  );
}

function computeInitials(name: string): string {
  if (!name?.trim()) {
    return 'U';
  }
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) {
    return parts[0].charAt(0).toUpperCase();
  }
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

// ==================== Contacts API ====================

export const contactsApi = {
  getAll: async (): Promise<Contact[]> => {
    const response = await fetch(`${API_BASE}/contacts`, {
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<Contact[]>(response);
  },

  getById: async (id: string): Promise<Contact> => {
    const response = await fetch(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<Contact>(response);
  },

  create: async (data: ContactRequest): Promise<Contact> => {
    const response = await fetch(`${API_BASE}/contacts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(data),
    });
    return handleResponse<Contact>(response);
  },

  update: async (id: string, data: Partial<ContactRequest>): Promise<Contact> => {
    const response = await fetch(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(data),
    });
    return handleResponse<Contact>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/contacts/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<void>(response);
  },
};

// ==================== Tasks API ====================

export const tasksApi = {
  getAll: async (): Promise<Task[]> => {
    const response = await fetch(`${API_BASE}/tasks`, {
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<Task[]>(response);
  },

  getById: async (id: string): Promise<Task> => {
    const response = await fetch(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<Task>(response);
  },

  create: async (data: TaskRequest): Promise<Task> => {
    const response = await fetch(`${API_BASE}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(data),
    });
    return handleResponse<Task>(response);
  },

  update: async (id: string, data: Partial<TaskRequest>): Promise<Task> => {
    const response = await fetch(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(data),
    });
    return handleResponse<Task>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/tasks/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<void>(response);
  },
};

// ==================== Appointments API ====================

export const appointmentsApi = {
  getAll: async (): Promise<Appointment[]> => {
    const response = await fetch(`${API_BASE}/appointments`, {
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<Appointment[]>(response);
  },

  getById: async (id: string): Promise<Appointment> => {
    const response = await fetch(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<Appointment>(response);
  },

  create: async (data: AppointmentRequest): Promise<Appointment> => {
    const response = await fetch(`${API_BASE}/appointments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(data),
    });
    return handleResponse<Appointment>(response);
  },

  update: async (id: string, data: Partial<AppointmentRequest>): Promise<Appointment> => {
    const response = await fetch(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(data),
    });
    return handleResponse<Appointment>(response);
  },

  delete: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/appointments/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { ...getAuthHeaders() },
    });
    return handleResponse<void>(response);
  },
};
