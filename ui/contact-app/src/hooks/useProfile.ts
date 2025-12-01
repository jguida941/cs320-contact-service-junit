import { useState, useEffect } from 'react';
import { authApi } from '@/lib/api';

export interface Profile {
  name: string;
  email: string;
  initials: string;
}

const PROFILE_KEY = 'contact-app-profile';

function buildDefaultProfile(): Profile {
  const user = authApi.getCurrentUser();
  const name = user?.username ?? 'User';
  const email = user?.email ?? '';
  return {
    name,
    email,
    initials: getInitials(name),
  };
}

function getInitials(name: string): string {
  if (!name.trim()) return 'U';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) {
    return parts[0].charAt(0).toUpperCase();
  }
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

export function useProfile() {
  const [profile, setProfileState] = useState<Profile>(() => {
    const stored = localStorage.getItem(PROFILE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return buildDefaultProfile();
      }
    }
    return buildDefaultProfile();
  });

  useEffect(() => {
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
  }, [profile]);

  const updateProfile = (updates: Partial<Omit<Profile, 'initials'>>) => {
    setProfileState((prev) => {
      const newName = updates.name ?? prev.name;
      return {
        ...prev,
        ...updates,
        initials: getInitials(newName),
      };
    });
  };

  const resetProfile = () => {
    setProfileState(buildDefaultProfile());
  };

  return {
    profile,
    updateProfile,
    resetProfile,
  };
}
