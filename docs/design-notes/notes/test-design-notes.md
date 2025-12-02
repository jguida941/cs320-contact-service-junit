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

## Test Isolation and Cleanup (Added 2025-12-02)

### The Problem We Solved
Early test suite development revealed a critical isolation issue: static singleton instances (`ContactService.getInstance()`, etc.) persisted across test executions while Spring Boot reused the same `ApplicationContext` for performance. This caused `DuplicateResourceException` failures when tests created resources with the same IDs - the errors were **order-dependent** and only appeared when running the full suite.

### Centralized Cleanup Solution
Created `TestCleanupUtility` component (in `contactapp.service` package) to enforce proper cleanup order:

```java
@Autowired
private TestCleanupUtility testCleanup;

@BeforeEach
void reset() {
    testCleanup.resetTestEnvironment();
}
```

**What it does (in order):**
1. **Clear security contexts** - Both `SecurityContextHolder` and `TestSecurityContextHolder`
2. **Reset singletons via reflection** - Sets static `instance` fields to `null` before data clearing
3. **Clean up test users** - Deletes users first to satisfy FK constraints
4. **Clear all service data** - Calls package-private `clearAllContacts()`, `clearAllTasks()`, etc.
5. **Setup fresh test user** - Ready for test execution

**Why this order matters:** If we clear data before resetting singletons, the next test's service initialization calls `registerInstance()` which migrates old singleton data into the fresh JPA store, recreating the DuplicateResource errors.

### Impact
- ✅ Eliminated 8 test failures across service test suites
- ✅ All 1066 tests now run reliably without order dependencies
- ✅ Single line in `@BeforeEach` replaces manual cleanup boilerplate
- ✅ Consistent isolation pattern across all Spring Boot integration tests

### Related Documentation
- **ADR-0047**: Complete architectural rationale and alternatives considered
- **ADR-0009**: Updated test strategy referencing centralized cleanup
- **CHANGELOG.md**: Historical context of the fix

### When Adding New Services
If you create a new service (e.g., `UserService`) with the singleton pattern:
1. Add `@Autowired(required = false) private UserService userService;` to `TestCleanupUtility`
2. Add `resetSingleton(UserService.class);` to the `resetAllSingletons()` method
3. Add `userService.clearAllUsers();` to the `clearAllServiceData()` method

This ensures all service tests benefit from consistent cleanup without duplicating reflection code.
