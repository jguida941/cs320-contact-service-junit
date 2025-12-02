import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  projectRequestSchema,
  ValidationLimits,
  ValidationLimitsProject,
  type ProjectRequest,
  type Project,
  type ProjectStatus,
} from '@/lib/schemas';

interface ProjectFormProps {
  project?: Project;
  onSubmit: (data: ProjectRequest) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

const STATUS_OPTIONS: { value: ProjectStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_HOLD', label: 'On Hold' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'ARCHIVED', label: 'Archived' },
];

export function ProjectForm({ project, onSubmit, onCancel, isLoading }: ProjectFormProps) {
  const isEdit = !!project;

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ProjectRequest>({
    resolver: zodResolver(projectRequestSchema),
    defaultValues: project
      ? {
          id: project.id,
          name: project.name,
          description: project.description,
          status: project.status,
        }
      : {
          status: 'ACTIVE',
        },
  });

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-4"
      aria-label={isEdit ? 'Edit project form' : 'Create project form'}
    >
      {isEdit ? (
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
        <Label htmlFor="name">Name</Label>
        <Input
          id="name"
          {...register('name')}
          placeholder="Enter project name"
          maxLength={ValidationLimitsProject.MAX_PROJECT_NAME_LENGTH}
          aria-invalid={errors.name ? 'true' : 'false'}
          aria-describedby={errors.name ? 'name-error name-help' : 'name-help'}
          required
        />
        {errors.name && (
          <p id="name-error" className="text-sm text-destructive" role="alert">
            {errors.name.message}
          </p>
        )}
        <p id="name-help" className="text-xs text-muted-foreground">
          Max {ValidationLimitsProject.MAX_PROJECT_NAME_LENGTH} characters
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Input
          id="description"
          {...register('description')}
          placeholder="Enter project description (optional)"
          maxLength={ValidationLimitsProject.MAX_PROJECT_DESCRIPTION_LENGTH}
          aria-invalid={errors.description ? 'true' : 'false'}
          aria-describedby={errors.description ? 'description-error description-help' : 'description-help'}
        />
        {errors.description && (
          <p id="description-error" className="text-sm text-destructive" role="alert">
            {errors.description.message}
          </p>
        )}
        <p id="description-help" className="text-xs text-muted-foreground">
          Max {ValidationLimitsProject.MAX_PROJECT_DESCRIPTION_LENGTH} characters
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="status">Status</Label>
        <Controller
          name="status"
          control={control}
          render={({ field }) => (
            <Select onValueChange={field.onChange} value={field.value}>
              <SelectTrigger
                id="status"
                aria-invalid={errors.status ? 'true' : 'false'}
                aria-describedby={errors.status ? 'status-error' : undefined}
              >
                <SelectValue placeholder="Select status" />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
        {errors.status && (
          <p id="status-error" className="text-sm text-destructive" role="alert">
            {errors.status.message}
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
