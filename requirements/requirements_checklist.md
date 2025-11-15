# CS-320 Module 3 - Requirements Checklist

## Contact Class

- [ ] contactId is required
- [ ] contactId is not null
- [ ] contactId length <= 10
- [ ] contactId is not updatable after construction

- [ ] firstName is required
- [ ] firstName is not null
- [ ] firstName length <= 10

- [ ] lastName is required
- [ ] lastName is not null
- [ ] lastName length <= 10

- [ ] phone is required
- [ ] phone is not null
- [ ] phone length == 10

- [ ] address is required
- [ ] address is not null
- [ ] address length <= 30

## ContactService

- [ ] addContact adds a contact with a unique contactId
- [ ] addContact rejects duplicate contactId
- [ ] deleteContact removes contact by contactId
- [ ] updateFirstName updates firstName by contactId and enforces validation
- [ ] updateLastName updates lastName by contactId and enforces validation
- [ ] updatePhone updates phone by contactId and enforces validation
- [ ] updateAddress updates address by contactId and enforces validation

## Tests

- [ ] Tests cover valid Contact creation
- [ ] Tests cover invalid contactId (null and >10)
- [ ] Tests cover invalid firstName, lastName, phone, address
- [ ] Tests cover adding a contact
- [ ] Tests cover rejecting duplicate contactId
- [ ] Tests cover deleting a contact
- [ ] Tests cover each update method