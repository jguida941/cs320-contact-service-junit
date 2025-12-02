import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { SearchInput } from '@/components/ui/search-input';
import { Pagination } from '@/components/ui/pagination';
import { SortableTableHead } from '@/components/ui/sortable-table-head';
import { useToast } from '@/hooks/useToast';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { Badge } from '@/components/ui/badge';
import { ContactForm } from '@/components/forms/ContactForm';
import { DeleteConfirmDialog } from '@/components/dialogs/DeleteConfirmDialog';
import { contactsApi } from '@/lib/api';
import { useFilteredSortedPaginatedData } from '@/lib/hooks/useTableState';
import type { Contact, ContactRequest } from '@/lib/schemas';

type SheetMode = 'view' | 'create' | 'edit';

/**
 * ContactsPage component with search, pagination, and sorting
 *
 * Features:
 * - Client-side search across all contact fields
 * - Pagination with 15 items per page
 * - Sortable columns (ID, Name, Phone, Address)
 * - URL query params for bookmarkable state
 * - Create/Edit/View/Delete operations
 */
export function ContactsPage() {
  const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
  const [sheetMode, setSheetMode] = useState<SheetMode>('view');
  const [deleteTarget, setDeleteTarget] = useState<Contact | null>(null);

  const queryClient = useQueryClient();
  const toast = useToast();

  // Fetch all contacts from API
  const { data: contacts = [], isLoading, error } = useQuery({
    queryKey: ['contacts'],
    queryFn: contactsApi.getAll,
  });

  // Apply search, sorting, and pagination to the contacts
  // Search across: id, firstName, lastName, phone, address
  const {
    items: paginatedContacts,
    totalItems,
    totalPages,
    currentPage,
    itemsPerPage,
    searchQuery,
    sortConfig,
    setSearch,
    setPage,
    setSort,
  } = useFilteredSortedPaginatedData<Contact>(contacts, [
    'id',
    'firstName',
    'lastName',
    'phone',
    'address',
  ]);

  const createMutation = useMutation({
    mutationFn: contactsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      closeSheet();
      toast.success('Contact created successfully');
    },
    onError: (error) => {
      toast.error(error);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<ContactRequest> }) =>
      contactsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      closeSheet();
      toast.success('Contact updated successfully');
    },
    onError: (error) => {
      toast.error(error);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: contactsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      setDeleteTarget(null);
      if (selectedContact?.id === deleteTarget?.id) {
        closeSheet();
      }
      toast.success('Contact deleted successfully');
    },
    onError: (error) => {
      toast.error(error);
    },
  });

  const openCreateSheet = () => {
    setSelectedContact(null);
    setSheetMode('create');
  };

  const openEditSheet = (contact: Contact) => {
    setSelectedContact(contact);
    setSheetMode('edit');
  };

  const openViewSheet = (contact: Contact) => {
    setSelectedContact(contact);
    setSheetMode('view');
  };

  const closeSheet = () => {
    setSelectedContact(null);
    setSheetMode('view');
  };

  const handleCreate = (data: ContactRequest) => {
    createMutation.mutate(data);
  };

  const handleUpdate = (data: ContactRequest) => {
    if (selectedContact) {
      updateMutation.mutate({ id: selectedContact.id, data });
    }
  };

  const handleDelete = () => {
    if (deleteTarget) {
      deleteMutation.mutate(deleteTarget.id);
    }
  };

  if (error) {
    return (
      <div className="flex items-center justify-center p-8">
        <p className="text-destructive">Failed to load contacts</p>
      </div>
    );
  }

  const isSheetOpen = sheetMode === 'create' || !!selectedContact;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Contacts</h2>
          <p className="text-muted-foreground">
            Manage your contacts and their information.
          </p>
        </div>
        <Button onClick={openCreateSheet}>
          <Plus className="mr-2 h-4 w-4" />
          Add Contact
        </Button>
      </div>

      {/* Search bar */}
      <SearchInput
        value={searchQuery}
        onChange={setSearch}
        placeholder="Search contacts by ID, name, phone, or address..."
        className="max-w-md"
      />

      {/* Table */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <SortableTableHead
                sortKey="id"
                currentSort={sortConfig}
                onSort={setSort}
              >
                ID
              </SortableTableHead>
              <SortableTableHead
                sortKey="firstName"
                currentSort={sortConfig}
                onSort={setSort}
              >
                Name
              </SortableTableHead>
              <SortableTableHead
                sortKey="phone"
                currentSort={sortConfig}
                onSort={setSort}
              >
                Phone
              </SortableTableHead>
              <SortableTableHead
                sortKey="address"
                currentSort={sortConfig}
                onSort={setSort}
              >
                Address
              </SortableTableHead>
              <TableHead className="w-[100px]">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center">
                  Loading...
                </TableCell>
              </TableRow>
            ) : totalItems === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">
                  {searchQuery
                    ? `No contacts found matching "${searchQuery}".`
                    : 'No contacts found. Add your first contact to get started.'}
                </TableCell>
              </TableRow>
            ) : (
              paginatedContacts.map((contact) => (
                <TableRow
                  key={contact.id}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => openViewSheet(contact)}
                >
                  <TableCell>
                    <Badge variant="outline">{contact.id}</Badge>
                  </TableCell>
                  <TableCell className="font-medium">
                    {contact.firstName} {contact.lastName}
                  </TableCell>
                  <TableCell>{contact.phone}</TableCell>
                  <TableCell>{contact.address}</TableCell>
                  <TableCell>
                    <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => openEditSheet(contact)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleteTarget(contact)}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setPage}
          itemsPerPage={itemsPerPage}
          totalItems={totalItems}
        />
      )}

      {/* Create/Edit/View Sheet */}
      <Sheet open={isSheetOpen} onOpenChange={(open) => !open && closeSheet()}>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>
              {sheetMode === 'create'
                ? 'New Contact'
                : sheetMode === 'edit'
                ? 'Edit Contact'
                : 'Contact Details'}
            </SheetTitle>
            <SheetDescription>
              {sheetMode === 'create'
                ? 'Add a new contact to your list.'
                : sheetMode === 'edit'
                ? 'Update the contact information.'
                : 'View contact information.'}
            </SheetDescription>
          </SheetHeader>

          {sheetMode === 'create' && (
            <div className="mt-6">
              <ContactForm
                onSubmit={handleCreate}
                onCancel={closeSheet}
                isLoading={createMutation.isPending}
              />
            </div>
          )}

          {sheetMode === 'edit' && selectedContact && (
            <div className="mt-6">
              <ContactForm
                contact={selectedContact}
                onSubmit={handleUpdate}
                onCancel={closeSheet}
                isLoading={updateMutation.isPending}
              />
            </div>
          )}

          {sheetMode === 'view' && selectedContact && (
            <div className="mt-6 space-y-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">ID</label>
                <p className="text-sm">{selectedContact.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">First Name</label>
                <p className="text-sm">{selectedContact.firstName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Last Name</label>
                <p className="text-sm">{selectedContact.lastName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Phone</label>
                <p className="text-sm">{selectedContact.phone}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Address</label>
                <p className="text-sm">{selectedContact.address}</p>
              </div>
              <div className="flex gap-2 pt-4">
                <Button onClick={() => openEditSheet(selectedContact)}>
                  <Pencil className="mr-2 h-4 w-4" />
                  Edit
                </Button>
                <Button
                  variant="destructive"
                  onClick={() => setDeleteTarget(selectedContact)}
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </Button>
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>

      {/* Delete Confirmation Dialog */}
      <DeleteConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Contact"
        description={`Are you sure you want to delete ${deleteTarget?.firstName} ${deleteTarget?.lastName}? This action cannot be undone.`}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
