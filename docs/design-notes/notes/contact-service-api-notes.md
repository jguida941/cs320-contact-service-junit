# ContactService singleton and storage

(Related: [ADR-0002](../../adrs/ADR-0002-contact-service-storage.md), [ContactService.java](../../src/main/java/contactapp/ContactService.java))

File: docs/design-notes/contact-service-storage-notes.md

## What problem this solves
- The app needs one central list of contacts so every caller sees the same state.
- A singleton keeps the store in one place instead of forcing callers to pass maps around.

## What the design is
- `ContactService` is a singleton accessed via `getInstance()`.
- It owns a `ConcurrentHashMap<String, Contact>` called `database`.
- Service API:
  - `addContact(Contact)` uses `putIfAbsent` to enforce unique IDs and returns `boolean`.
  - `deleteContact(String id)` validates the id then calls `remove`, returning `boolean`.
  - `updateContact(String id, ...)` fetches the contact and delegates to `Contact.update(...)` so validation stays centralized.
  - `getDatabase()` returns `Map.copyOf(database)` for a read-only snapshot.
  - `clearAllContacts()` wipes the store so tests can reset between runs.

## Why this design
### Why `ConcurrentHashMap`
- Acts like a normal `HashMap` but allows concurrent access without explicit locking for get/put/remove.
- O(1) average time for add/update/delete operations.

### Why `putIfAbsent`
- Combines the uniqueness check and insert into one atomic call.
- If the id is new, it inserts and returns `null`, so we know the add succeeded.
- If the id exists, it leaves the map untouched and we return `false`.

### Why `Map.copyOf` and `clearAllContacts`
- If `getDatabase()` returned the real `database`, a caller could run `service.getDatabase().clear()` or `put()` and bypass every rule in the service API.  
  That is why we return `Map.copyOf(database)`: it makes a **separate, unmodifiable** snapshot.  
  Callers can read entries but any attempt to `put` or `clear` throws `UnsupportedOperationException`, so the internal map stays protected.
- `clearAllContacts()` exists because the service is a singleton shared across tests.  
  Without it, a contact added in one JUnit test would still be present in the next test.  
  Tests call `ContactService.getInstance().clearAllContacts()` (usually in `@BeforeEach`) to guarantee a clean database every time.

### Why booleans for service methods
- `add/delete/update` returning `boolean` keeps the API simple: success vs duplicate/missing.
- Tests assert on those booleans to cover the duplicate/missing branches without needing exceptions for normal control flow.
