import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { taskRequestSchema, ValidationLimits, type TaskRequest, type Task } from '@/lib/schemas';

interface TaskFormProps {
  task?: Task;
  onSubmit: (data: TaskRequest) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function TaskForm({ task, onSubmit, onCancel, isLoading }: TaskFormProps) {
  const isEdit = !!task;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TaskRequest>({
    resolver: zodResolver(taskRequestSchema),
    defaultValues: task
      ? {
          id: task.id,
          name: task.name,
          description: task.description,
        }
      : undefined,
  });

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-4"
      aria-label={isEdit ? 'Edit task form' : 'Create task form'}
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
        <Label htmlFor="name">Name</Label>
        <Input
          id="name"
          {...register('name')}
          placeholder="Enter task name"
          maxLength={ValidationLimits.MAX_TASK_NAME_LENGTH}
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
          Max {ValidationLimits.MAX_TASK_NAME_LENGTH} characters
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Input
          id="description"
          {...register('description')}
          placeholder="Enter task description"
          maxLength={ValidationLimits.MAX_DESCRIPTION_LENGTH}
          aria-invalid={errors.description ? 'true' : 'false'}
          aria-describedby={errors.description ? 'description-error description-help' : 'description-help'}
          required
        />
        {errors.description && (
          <p id="description-error" className="text-sm text-destructive" role="alert">
            {errors.description.message}
          </p>
        )}
        <p id="description-help" className="text-xs text-muted-foreground">
          Max {ValidationLimits.MAX_DESCRIPTION_LENGTH} characters
        </p>
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
