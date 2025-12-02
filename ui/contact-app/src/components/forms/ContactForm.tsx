import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { contactRequestSchema, ValidationLimits, type ContactRequest, type Contact } from '@/lib/schemas';

interface ContactFormProps {
  contact?: Contact;
  onSubmit: (data: ContactRequest) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function ContactForm({ contact, onSubmit, onCancel, isLoading }: ContactFormProps) {
  const isEdit = !!contact;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ContactRequest>({
    resolver: zodResolver(contactRequestSchema),
    defaultValues: contact
      ? {
          id: contact.id,
          firstName: contact.firstName,
          lastName: contact.lastName,
          phone: contact.phone,
          address: contact.address,
        }
      : undefined,
  });

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-4"
      aria-label={isEdit ? 'Edit contact form' : 'Create contact form'}
    >
      {isEdit ? (
        // Hidden input to include ID in edit submissions
        <input type="hidden" {...register('id')} />
      ) : (
        <div className="space-y-2">
          <Label htmlFor="id">ID</Label>
          <Input
            id="id"
            {...register('id')}
            placeholder="Unique ID (max 10 chars)"
            maxLength={ValidationLimits.MAX_ID_LENGTH}
            aria-invalid={errors.id ? 'true' : 'false'}
            aria-describedby={errors.id ? 'id-error' : undefined}
          />
          {errors.id && (
            <p id="id-error" className="text-sm text-destructive" role="alert">
              {errors.id.message}
            </p>
          )}
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="firstName">First Name</Label>
        <Input
          id="firstName"
          {...register('firstName')}
          placeholder="Enter first name"
          maxLength={ValidationLimits.MAX_NAME_LENGTH}
          aria-invalid={errors.firstName ? 'true' : 'false'}
          aria-describedby={errors.firstName ? 'firstName-error' : undefined}
          required
        />
        {errors.firstName && (
          <p id="firstName-error" className="text-sm text-destructive" role="alert">
            {errors.firstName.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="lastName">Last Name</Label>
        <Input
          id="lastName"
          {...register('lastName')}
          placeholder="Enter last name"
          maxLength={ValidationLimits.MAX_NAME_LENGTH}
          aria-invalid={errors.lastName ? 'true' : 'false'}
          aria-describedby={errors.lastName ? 'lastName-error' : undefined}
          required
        />
        {errors.lastName && (
          <p id="lastName-error" className="text-sm text-destructive" role="alert">
            {errors.lastName.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="phone">Phone</Label>
        <Input
          id="phone"
          type="tel"
          {...register('phone')}
          placeholder="1234567890"
          maxLength={ValidationLimits.PHONE_LENGTH}
          aria-invalid={errors.phone ? 'true' : 'false'}
          aria-describedby={errors.phone ? 'phone-error phone-help' : 'phone-help'}
          required
        />
        {errors.phone && (
          <p id="phone-error" className="text-sm text-destructive" role="alert">
            {errors.phone.message}
          </p>
        )}
        <p id="phone-help" className="text-xs text-muted-foreground">
          Must be exactly {ValidationLimits.PHONE_LENGTH} digits
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="address">Address</Label>
        <Input
          id="address"
          {...register('address')}
          placeholder="Enter address"
          maxLength={ValidationLimits.MAX_ADDRESS_LENGTH}
          aria-invalid={errors.address ? 'true' : 'false'}
          aria-describedby={errors.address ? 'address-error' : undefined}
          required
        />
        {errors.address && (
          <p id="address-error" className="text-sm text-destructive" role="alert">
            {errors.address.message}
          </p>
        )}
      </div>

      <div className="flex justify-end gap-2 pt-4">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isLoading}>
          {isLoading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
        </Button>
      </div>
    </form>
  );
}
