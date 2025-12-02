# ADR-0035: Boolean Return API for Service Methods

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: ADR-0002

## Context
- Service methods like `addContact()`, `deleteContact()`, `updateContact()` can fail for expected reasons (duplicate ID, not found).
- We needed to decide: throw exceptions or return success/failure?

## Decision
Return `boolean` for expected failures, throw exceptions for unexpected ones.

```java
public boolean addContact(Contact contact) {
    if (contact == null) throw new IllegalArgumentException();  // Unexpected - bug
    if (store.existsById(contact.getId())) return false;        // Expected - duplicate
    store.save(contact);
    return true;
}
```

## Why Booleans?
| Approach                | Pros                 | Cons                            |
|-------------------------|----------------------|---------------------------------|
| Always throw            | Clear failure reason | Try-catch everywhere, expensive |
| Always return boolean   | Simple, fast         | Less detail on failure          |
| **Hybrid (our choice)** | Best of both         | Must document which is which    |

## The Rule
- **Return false**: Expected business cases (duplicate ID, not found)
- **Throw exception**: Bugs or unexpected state (null input, invalid data)

## Interview Explanation
"We return booleans for expected failures like 'ID already exists' - the controller can easily check and return 409. We throw exceptions for unexpected things like null input - that's a bug that should crash fast."