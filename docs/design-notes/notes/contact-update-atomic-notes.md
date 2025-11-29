# Update atomicity pattern

(Related: [ADR-0003](../../adrs/ADR-0003-update-atomicity.md), [Contact.java](../../../src/main/java/contactapp/domain/Contact.java))

File: docs/design-notes/notes/contact-update-atomic-notes.md

## What problem this solves
- Updating fields one by one could leave a `Contact` in a half-valid state if a later setter throws.
- Tests would have to compensate for partially updated objects, which is confusing and brittle.
- **Example:** if we ran `contact.setFirstName("Alice")` (passes) and then `contact.setPhone("12345")` (fails), the `Contact` would keep `firstName = "Alice"` but `phone` would remain the old value.  
  You now have a partially updated object and no easy way to roll it back.

## What the design is
- `Contact.update(String newFirst, String newLast, String newPhone, String newAddress)` validates every new value up front using the same helper methods the constructor/setters call.
- Only after all validations succeed does it assign all four fields, so the change is “all or nothing” for the object (logical atomicity, not about threads/locks).
- **Example:** calling `contact.update("Alice", "Smith", "1234567890", "42 Main St")` validates every value before touching the object.  
  If all values pass, it sets all four fields together in a single block.

## Why this is safer
- If any new value is invalid, `Contact.update(...)` throws an `IllegalArgumentException` and **none** of the fields are changed, so the `Contact` never sits in a broken state.
- If everything is valid, all four fields update together in one step.
- Service code such as `ContactService.updateContact(...)` can simply call the helper once and inherit this all-or-nothing behavior instead of chaining setters.
- **Example:** `contact.update("Alice", "Smith", "12345", "42 Main St")` fails because the phone number is not 10 digits.  
  The method throws and `firstName`, `lastName`, and `address` stay untouched.

## Where it is in the code
- `Contact#update(...)` is the atomic helper.
- `ContactService#updateContact(...)` delegates directly to that helper instead of chaining setters.

## Simple Summary
Instead of updating each field separately and risking a half-updated contact if one value is bad, I use a single `update(...)` method that validates all the new values first and only applies them if they are all valid.
This gives me all-or-nothing updates and keeps the service code simple.
