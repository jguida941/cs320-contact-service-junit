import { useQuery } from '@tanstack/react-query';
import { Users, Database, Activity, CheckSquare, Calendar, UserCheck, Shield } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { adminApi } from '@/lib/api';
import { ScrollArea } from '@/components/ui/scroll-area';

/**
 * Admin Dashboard Page
 *
 * <p>Displays system-wide metrics, user management, and audit logs.
 * Only accessible to users with ADMIN role per ADR-0036.
 *
 * <p>Features:
 * - System metrics (total users, contacts, tasks, appointments)
 * - User management table (list all users with their roles)
 * - Recent activity audit log
 */
export function AdminDashboard() {
  // Fetch system-wide statistics
  const { data: stats, isLoading: statsLoading, error: statsError } = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: adminApi.getSystemStats,
    // Refresh every 30 seconds
    refetchInterval: 30000,
  });

  // Fetch all users
  const { data: users = [], isLoading: usersLoading, error: usersError } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: adminApi.getAllUsers,
  });

  // Fetch audit log
  const { data: auditLog = [], isLoading: auditLoading, error: auditError } = useQuery({
    queryKey: ['admin', 'audit-log'],
    queryFn: () => adminApi.getAuditLog(50),
    // Refresh every minute
    refetchInterval: 60000,
  });

  // System metrics cards
  const systemStats = stats ? [
    {
      title: 'Total Users',
      value: stats.totalUsers,
      icon: Users,
      description: 'Registered accounts',
      color: 'text-blue-600',
    },
    {
      title: 'Active Users',
      value: stats.activeUsers,
      icon: UserCheck,
      description: 'Recently active',
      color: 'text-green-600',
    },
    {
      title: 'Total Contacts',
      value: stats.totalContacts,
      icon: Database,
      description: 'Across all users',
      color: 'text-purple-600',
    },
    {
      title: 'Total Tasks',
      value: stats.totalTasks,
      icon: CheckSquare,
      description: 'Across all users',
      color: 'text-orange-600',
    },
    {
      title: 'Total Appointments',
      value: stats.totalAppointments,
      icon: Calendar,
      description: 'Across all users',
      color: 'text-pink-600',
    },
  ] : [];

  // Format timestamp for display
  const formatTimestamp = (timestamp: string): string => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString();
    } catch {
      return timestamp;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center gap-2">
        <Shield className="h-6 w-6 text-primary" />
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Admin Dashboard</h2>
          <p className="text-muted-foreground">
            System-wide metrics, user management, and activity monitoring
          </p>
        </div>
      </div>

      {/* System Metrics */}
      <div>
        <h3 className="text-lg font-semibold mb-4">System Metrics</h3>
        {statsError ? (
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-destructive">
                Failed to load system statistics. Please try again later.
              </p>
            </CardContent>
          </Card>
        ) : statsLoading ? (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <Card key={i}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Loading...</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">-</div>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {systemStats.map((stat) => (
              <Card key={stat.title}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
                  <stat.icon className={`h-4 w-4 ${stat.color}`} />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stat.value}</div>
                  <p className="text-xs text-muted-foreground">{stat.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* User Management */}
      <Card>
        <CardHeader>
          <CardTitle>User Management</CardTitle>
          <CardDescription>All registered users and their roles</CardDescription>
        </CardHeader>
        <CardContent>
          {usersError ? (
            <p className="text-sm text-destructive">
              Failed to load users. Please try again later.
            </p>
          ) : usersLoading ? (
            <p className="text-sm text-muted-foreground">Loading users...</p>
          ) : users.length === 0 ? (
            <p className="text-sm text-muted-foreground">No users found.</p>
          ) : (
            <ScrollArea className="h-[300px]">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Created At</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell className="font-medium">{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>
                        <Badge variant={user.role === 'ADMIN' ? 'default' : 'secondary'}>
                          {user.role}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatTimestamp(user.createdAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </ScrollArea>
          )}
        </CardContent>
      </Card>

      {/* Recent Activity / Audit Log */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Activity className="h-4 w-4" />
            <CardTitle>Recent Activity</CardTitle>
          </div>
          <CardDescription>System-wide audit log of recent actions</CardDescription>
        </CardHeader>
        <CardContent>
          {auditError ? (
            <p className="text-sm text-destructive">
              Failed to load audit log. Please try again later.
            </p>
          ) : auditLoading ? (
            <p className="text-sm text-muted-foreground">Loading activity...</p>
          ) : auditLog.length === 0 ? (
            <p className="text-sm text-muted-foreground">No recent activity.</p>
          ) : (
            <ScrollArea className="h-[400px]">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>User</TableHead>
                    <TableHead>Action</TableHead>
                    <TableHead>Resource</TableHead>
                    <TableHead>Resource ID</TableHead>
                    <TableHead>Timestamp</TableHead>
                    <TableHead>Details</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {auditLog.map((entry) => (
                    <TableRow key={entry.id}>
                      <TableCell className="font-medium">{entry.username}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{entry.action}</Badge>
                      </TableCell>
                      <TableCell>{entry.resourceType}</TableCell>
                      <TableCell className="font-mono text-xs">{entry.resourceId}</TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatTimestamp(entry.timestamp)}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {entry.details || '-'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </ScrollArea>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
