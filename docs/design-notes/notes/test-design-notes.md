# Test design and tooling

(Related: [ContactTest.java](../../../src/test/java/contactapp/domain/ContactTest.java), [ValidationTest.java](../../../src/test/java/contactapp/domain/ValidationTest.java), [ContactServiceTest.java](../../../src/test/java/contactapp/service/ContactServiceTest.java))

File: docs/design-notes/notes/test-design-notes.md

## Why the tests are split
- `ContactTest` covers the `Contact` class (constructor, setters, update helper) so we know the object enforces all rules, including invalid update atomicity.
- `ValidationTest` isolates the helper methods to verify boundary cases (min/max lengths, blank inputs) without creating an entire `Contact`.
- `ContactServiceTest` focuses on the service API (add/delete/update behavior, singleton reset).
- `TaskTest`/`TaskServiceTest` mirror the Contact coverage now that Task is implemented: constructor trimming, invalid inputs (including update atomicity), singleton behavior, and CRUD flows.

## Why AssertJ and parameterized tests
- AssertJ (`assertThat`, `assertThatThrownBy`) reads like plain English: `assertThat(contact.getFirstName()).isEqualTo("Alice")` is easy to follow and can chain multiple checks.
- Parameterized tests with `@CsvSource` cover many invalid input combinations in one method, reducing copy/paste and making it simple to add new cases.
  - Example: `ContactTest.testFailedCreation(...)` lists invalid inputs and expected error messages in one place instead of writing dozens of separate test methods.

## How this helps
- Each test class has a clear responsibility, so failures pinpoint whether the issue is in the domain, validation helper, or service layer.
- Surefire automatically runs every `*Test` class during `mvn verify`, so even newly added tests are picked up without manual configuration.
- The fluent assertions and parameterized inputs keep the suite small but expressive, making it easy for instructors or teammates to see which cases are covered.
