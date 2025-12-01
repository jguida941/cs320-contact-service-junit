import { useState } from 'react';
import { Sun, Moon, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useTheme } from '@/hooks/useTheme';
import { useProfile } from '@/hooks/useProfile';
import { cn } from '@/lib/utils';

const themeColors: Record<string, { primary: string; label: string }> = {
  slate: { primary: 'bg-slate-600', label: 'Slate' },
  ocean: { primary: 'bg-blue-600', label: 'Ocean' },
  forest: { primary: 'bg-green-600', label: 'Forest' },
  violet: { primary: 'bg-violet-600', label: 'Violet' },
  zinc: { primary: 'bg-zinc-600', label: 'Zinc' },
};

export function SettingsPage() {
  const { theme, setTheme, themes, darkMode, toggleDarkMode } = useTheme();
  const { profile, updateProfile, resetProfile } = useProfile();

  const [name, setName] = useState(profile.name);
  const [email, setEmail] = useState(profile.email);
  const [saved, setSaved] = useState(false);

  const handleSaveProfile = () => {
    updateProfile({ name, email });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const hasProfileChanges = name !== profile.name || email !== profile.email;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Settings</h2>
        <p className="text-muted-foreground">
          Manage your account settings and preferences.
        </p>
      </div>

      <Separator />

      {/* Profile Section */}
      <Card>
        <CardHeader>
          <CardTitle>Profile</CardTitle>
          <CardDescription>
            Update your personal information.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
              />
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button
              onClick={handleSaveProfile}
              disabled={!hasProfileChanges}
            >
              {saved ? (
                <>
                  <Check className="mr-2 h-4 w-4" />
                  Saved
                </>
              ) : (
                'Save Profile'
              )}
            </Button>
            {hasProfileChanges && (
              <span className="text-sm text-muted-foreground">
                You have unsaved changes
              </span>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Appearance Section */}
      <Card>
        <CardHeader>
          <CardTitle>Appearance</CardTitle>
          <CardDescription>
            Customize the look and feel of the application.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Dark Mode Toggle */}
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>Dark Mode</Label>
              <p className="text-sm text-muted-foreground">
                Switch between light and dark themes.
              </p>
            </div>
            <Button
              variant="outline"
              size="icon"
              onClick={toggleDarkMode}
              className="h-10 w-10"
            >
              {darkMode ? (
                <Sun className="h-5 w-5" />
              ) : (
                <Moon className="h-5 w-5" />
              )}
            </Button>
          </div>

          <Separator />

          {/* Theme Selection */}
          <div className="space-y-3">
            <div>
              <Label>Color Theme</Label>
              <p className="text-sm text-muted-foreground">
                Choose a color scheme for the interface.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
              {themes.map((t) => {
                const colors = themeColors[t];
                const isSelected = theme === t;
                return (
                  <button
                    key={t}
                    onClick={() => setTheme(t)}
                    className={cn(
                      'flex flex-col items-center gap-2 rounded-lg border-2 p-3 transition-all',
                      'hover:border-primary/50',
                      isSelected
                        ? 'border-primary bg-accent'
                        : 'border-border'
                    )}
                  >
                    <div
                      className={cn(
                        'h-8 w-8 rounded-full',
                        colors.primary
                      )}
                    />
                    <span className="text-sm font-medium">{colors.label}</span>
                    {isSelected && (
                      <Check className="h-4 w-4 text-primary" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Data Section */}
      <Card>
        <CardHeader>
          <CardTitle>Data</CardTitle>
          <CardDescription>
            Manage your application data.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>Clear Local Settings</Label>
              <p className="text-sm text-muted-foreground">
                Reset theme preferences and profile to defaults.
              </p>
            </div>
            <Button
              variant="outline"
              onClick={() => {
                // Only clear settings-related items, preserve auth tokens
                localStorage.removeItem('theme');
                localStorage.removeItem('darkMode');
                localStorage.removeItem('user_profile');
                resetProfile();
                window.location.reload();
              }}
            >
              Clear Settings
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
