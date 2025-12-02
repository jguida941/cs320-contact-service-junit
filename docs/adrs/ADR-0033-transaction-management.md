# ADR-0033: Transaction Management with @Transactional

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: ADR-0024

## Context
- Database operations can fail partway through (network issues, constraint violations, crashes).
- Multi-step operations (check if exists, then save) can have race conditions.
- We needed a strategy to keep data consistent when things go wrong.

## Decision
Use Spring's `@Transactional` annotation on service methods to ensure database operations are atomic.

## What @Transactional Actually Does

### Without @Transactional (dangerous)
```java
public boolean addContact(Contact contact) {
    if (store.existsById(contact.getId())) {  // Step 1: Check
        return false;
    }
    store.save(contact);  // Step 2: Save
    return true;
}
```
**Problem**: Between Step 1 and Step 2, another request could insert the same ID. You'd get a constraint violation or duplicate data.

### With @Transactional (safe)
```java
@Transactional
public boolean addContact(Contact contact) {
    if (store.existsById(contact.getId())) {  // Step 1
        return false;
    }
    store.save(contact);  // Step 2
    return true;
}  // Transaction commits here - all or nothing
```
**Guarantee**: Steps 1 and 2 happen as a single atomic unit. If Step 2 fails, Step 1's read is also "rolled back" (logically - no stale data committed).

## The Bank Transfer Analogy

```java
@Transactional
public void transfer(Account from, Account to, int amount) {
    from.withdraw(amount);   // Step 1: Take money out
    to.deposit(amount);      // Step 2: Put money in
}
```

- **Without transaction**: If Step 2 fails, money disappears (taken from `from` but never added to `to`)
- **With transaction**: If Step 2 fails, Step 1 is rolled back - money stays in original account

## Read-Only Optimization

```java
@Transactional(readOnly = true)
public List<Contact> getAllContacts() {
    return store.findAll();
}
```

- Tells the database "I'm only reading, not writing"
- Database can optimize (skip write locks, use read replicas)
- Documents intent to other developers

## When We Use It

| Method Type        | @Transactional | Why                            |
|--------------------|----------------|--------------------------------|
| `addContact()`     | Yes            | Check + save must be atomic    |
| `updateContact()`  | Yes            | Find + update must be atomic   |
| `deleteContact()`  | Yes            | Consistency with other writes  |
| `getAllContacts()` | Yes (readOnly) | Optimization, consistent reads |
| `getContactById()` | Yes (readOnly) | Optimization                   |

## Consequences
- **Positive**: Data stays consistent even when operations fail
- **Positive**: Race conditions between check-and-save are prevented
- **Positive**: Automatic rollback on exceptions
- **Negative**: Slight performance overhead (transaction management)
- **Negative**: Must understand transaction boundaries

## Interview Explanation
"@Transactional ensures that multi-step database operations are atomic - they either all succeed or all fail together. Like a bank transfer: you don't want money to leave one account without arriving in the other. If any step fails, the whole operation rolls back to the previous state. We also use readOnly=true for queries to let the database optimize since we're not modifying data."