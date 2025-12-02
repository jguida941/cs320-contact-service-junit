# ADR-0034: Mapper Pattern for Entity-Domain Separation

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: ADR-0024, ADR-0032

## Context
- We have domain objects (`Contact`, `Task`, `Appointment`) with validation and business logic.
- We have JPA entities (`ContactEntity`, `TaskEntity`, `AppointmentEntity`) for database persistence.
- We needed to decide: use domain objects directly as JPA entities, or keep them separate?

## Decision
Keep domain and JPA entities **separate**, connected by **mapper components**.

## Why Separate?
- JPA needs a no-arg constructor; our domain validation runs in constructor
- Hibernate creates proxy objects that bypass constructors
- Keeping them separate means domain stays pure (no @Entity annotations)

## The Key Insight: Re-Validation on Load
When we load from database, the mapper calls the domain constructor - so validation runs again. Data corruption in DB is caught immediately.

## Reconstitution Constructor Pattern (Task)
Task includes a specialized 7-argument constructor for reconstituting from persistence:
```java
public Task(String taskId, String name, String description,
            TaskStatus status, LocalDate dueDate,
            Instant createdAt, Instant updatedAt)
```

This constructor:
- Accepts timestamps from the entity instead of generating new ones
- Validates all domain constraints (just like the standard constructor)
- Prevents timestamp drift between domain and entity layers

When `TaskMapper.toDomain()` loads from database, it uses this constructor to preserve the entity's original timestamps. This ensures the domain object accurately reflects the persistence state, preventing subtle bugs where domain timestamps diverge from entity timestamps.

Contact and Appointment don't track timestamps, so they use simpler constructors.

## Interview Explanation
"We keep domain objects and JPA entities separate so the domain stays pure. The mapper calls the domain constructor when loading, so validation always runs - even on database data. For Task, we use a special reconstitution constructor that accepts timestamps from the database, preventing timestamp drift between layers."