# Documentation Audit Findings
**Date**: 2025-12-02
**Auditor**: Claude Code
**Purpose**: Identify stale, outdated, or missing documentation following recent architectural changes

---

## Executive Summary

This audit identified **critical inconsistencies and omissions** across the project's main documentation files (README.md, REQUIREMENTS.md, ROADMAP.md, PROJECT_SUMMARY.md). The most significant issues include:

1. **Test count discrepancies** - Different documents claim 949, 1,026, and 1,066 tests
2. **Mutation score inconsistencies** - Documents cite 89%, 92%, 94%, and 99% mutation scores
3. **Missing documentation for recent changes** - SpaCsrfTokenRequestHandler, frontend fixes, and test isolation strategies are undocumented
4. **Inconsistent singleton test status** - Some docs say tests "can be run separately" while code shows they're @Disabled
5. **Unclear forkCount=1 implications** - Not explained why this was needed or its impact

---

## Critical Issues

### 1. Test Count Discrepancies

**Impact**: HIGH - Makes it impossible to verify CI/CD claims

| Document | Line | Claim | Status |
|----------|------|-------|--------|
| README.md | 17 | "1,066 test executions (911 `@Test` methods)" | Needs verification |
| README.md | 135 | "1066 tests" | Duplicate claim |
| REQUIREMENTS.md | 47 | "949 tests" | Contradicts README |
| REQUIREMENTS.md | 48 | "1,066 tests" | Matches README but contradicts line 47 |
| REQUIREMENTS.md | 52 | "1,026 tests passing" | Third different number |
| ROADMAP.md | 10 | "949 tests" | Contradicts README |
| ROADMAP.md | 37 | "Total test count: 1,066" | Contradicts line 10 |
| PROJECT_SUMMARY.md | 108 | "949 tests" | Contradicts README |

**Recommendation**: Run `mvn test` and count actual test executions, then update ALL documents to use the same number consistently.

### 2. Mutation Score Discrepancies

**Impact**: HIGH - Quality metrics are unreliable

| Document | Line | Claim | Status |
|----------|------|-------|--------|
| README.md | 17 | "roughly 89% mutation score" | One claim |
| README.md (implied) | Various | "94%" mentioned elsewhere | Different claim |
| REQUIREMENTS.md | 52 | "94% mutation score" | Contradicts README |
| PROJECT_SUMMARY.md | 108 | "94% mutation coverage" | Matches REQUIREMENTS |
| PROJECT_SUMMARY.md | 332 | "99% kill rate" | VERY different from others |

**Recommendation**: Run PITest and document the actual mutation score once. Use that number everywhere.

### 3. Missing: SpaCsrfTokenRequestHandler

**Impact**: HIGH - Security fix is undocumented

**What Changed**:
- Added `SpaCsrfTokenRequestHandler.java` at `src/main/java/contactapp/security/SpaCsrfTokenRequestHandler.java`
- Fixes Spring Security 6 CSRF compatibility for SPA applications
- Accepts raw CSRF tokens from cookies while providing BREACH protection via XOR masking
- Recent Checkstyle fix wrapped long URL in javadoc

**Currently Documented**: NOWHERE in main documentation

**Files That Should Mention It**:
- README.md (Security Infrastructure section around line 923-999)
- REQUIREMENTS.md (Phase 5 Security section around lines 197-203)
- PROJECT_SUMMARY.md (Security Decision section around lines 347-355)

**Recommendation**: Add entry to Security Infrastructure section:
```markdown
### [SpaCsrfTokenRequestHandler.java](src/main/java/contactapp/security/SpaCsrfTokenRequestHandler.java)
- Custom CSRF token request handler for Single Page Applications (Spring Security 6 migration)
- Accepts raw CSRF tokens from cookies (as sent by SPAs that read XSRF-TOKEN cookie)
- Provides BREACH protection via XOR masking for server-rendered pages
- Based on Spring Security 6 migration guide for `CookieCsrfTokenRepository` compatibility
```

### 4. Missing: Frontend Form Fixes

**Impact**: MEDIUM - User-facing bug fixes are undocumented

**What Changed**:
- Added `noValidate` attribute to all forms in AppointmentForm.tsx and TaskForm.tsx
- Removed empty `SelectItem` values that caused blank screen issues
- These fixes resolved:
  - Blank screen when clicking Task/Appointment buttons
  - Date input editing problems (HTML5 validation conflicts)

**Currently Documented**: NOWHERE

**Recommendation**: Add to React UI Highlights section (around line 193-200 in README.md):
```markdown
- **Form validation fixes**:
  - Added `noValidate` attribute to forms to prevent HTML5 validation conflicts with React Hook Form + Zod
  - Removed empty SelectItem values from dropdowns to fix blank screen rendering issues
  - Ensures smooth date editing and proper form submission behavior
```

### 5. Missing: Test Execution Strategy Details

**Impact**: HIGH - Developers don't understand why tests are configured this way

**What Changed**:
- `pom.xml` line 312: Set `forkCount=1` to run all tests in single JVM
- This was needed because previous parallel execution caused test hangs
- Side effect: Singleton state persists across tests
- Consequence: Singleton tests had to be @Disabled to avoid duplicate ID errors

**Current Documentation**:
- README.md line 18: Mentions tests "can be run separately" but doesn't say they're disabled
- README.md line 132: Mentions `-DskipTestcontainersTests=true` for Windows
- REQUIREMENTS.md line 53: Says tests "can be run separately" but not that they're disabled

**Missing**:
- Why `forkCount=1` was needed (test hangs)
- What the tradeoff is (singleton state persistence)
- Why singleton tests are @Disabled (duplicate IDs with forkCount=1)
- How to run tests properly on different platforms

**Recommendation**: Add clarifying section to README.md and REQUIREMENTS.md:
```markdown
### Test Execution Configuration

**Test Hang Fix (forkCount=1)**:
- Maven Surefire configured with `forkCount=1` to prevent test hangs that occurred with parallel execution
- Tradeoff: All tests run in single JVM, so singleton state persists across test classes
- Consequence: Singleton-sharing tests (`testSingletonSharesStateWithSpringBean` in *ServiceTest.java) are @Disabled to prevent duplicate ID errors

**Platform-Specific Strategies**:
- **Linux CI**: Runs full suite with Testcontainers/Postgres
- **Windows CI**: Uses `skip-testcontainers` profile with H2 (no Docker required)
- **Legacy singleton tests**: Tagged `legacy-singleton`, currently @Disabled due to forkCount=1 state persistence
  - Can be enabled individually for debugging: `mvn test -Dtest=ContactServiceLegacyTest#specificTest`
  - Full legacy suite via `mvn test -Plegacy-singleton` (requires re-enabling @Disabled annotations first)
```

### 6. Singleton Test Status Confusion

**Impact**: MEDIUM - Unclear what tests actually run

**Current State** (verified from code):
- `ContactServiceTest.java:55` - `@Disabled("Temporarily disabled singleton path to isolate duplicate-id flakiness")`
- `TaskServiceTest.java` - Has @Disabled on singleton test
- `AppointmentServiceTest.java` - Has @Disabled on singleton test
- `ProjectServiceTest.java` - Has @Disabled on singleton test
- `ContactServiceLegacyTest.java` - NO @Disabled at class level (contrary to earlier claims)
- Other *LegacyTest.java files - NO @Disabled annotations found

**Documentation Claims**:
- README.md line 18: "can be run separately via `mvn test -Plegacy-singleton`" (implies they're runnable)
- README.md line 133: Same claim
- REQUIREMENTS.md line 53: Same claim
- ROADMAP.md line 39: Same claim

**Reality**: Only 4 specific tests in *ServiceTest.java files are @Disabled, not entire LegacyTest classes

**Recommendation**: Clarify that:
1. Legacy test classes are NOT disabled
2. Only specific `testSingletonSharesStateWithSpringBean` methods are disabled
3. Update all docs to reflect actual state

### 7. Missing: TestCleanupUtility

**Impact**: LOW - Important utility is barely documented

**What It Is**:
- `src/test/java/contactapp/service/TestCleanupUtility.java`
- Resets SecurityContext, reseeds test users, resets singleton instances
- Critical for test isolation with forkCount=1

**Currently Documented**:
- README.md mentions it ONCE (found via grep)
- Not mentioned in REQUIREMENTS.md
- Not mentioned in PROJECT_SUMMARY.md

**Recommendation**: Add to test strategy documentation explaining its role in maintaining test isolation.

### 8. Project Phase 6 Status Inconsistency

**Impact**: MEDIUM - Unclear if feature is complete

| Document | Line | Claim | Status |
|----------|------|-------|--------|
| ROADMAP.md | 35 | "Phase 6 (Contact-Project Linking): Deferred to future" | Says deferred |
| REQUIREMENTS.md | 483 | "**Phase 6 Complete (2025-12-02)**" with checkmarks | Says complete |
| README.md | 191 | "**Phase 6 Implementation**: Contact-Project Linking is fully implemented" | Says complete |

**Recommendation**: Update ROADMAP.md to match reality - Phase 6 is complete, not deferred.

---

## Medium Priority Issues

### 9. PostgresContainerSupport @DynamicPropertySource Removal

**What Changed**: Removed `@DynamicPropertySource` from `PostgresContainerSupport.java`, now uses only `@ServiceConnection`

**Currently Documented**: Not mentioned in recent changes or architecture decisions

**Recommendation**: Add note to ADR-0048 or create new ADR documenting the Testcontainers configuration evolution.

### 10. Inconsistent Coverage Percentages

Multiple coverage percentages mentioned throughout docs without clear context:
- "about 92% line coverage" (README.md:17)
- "96%+ line coverage on stores, 95%+ on mappers" (README.md:135)
- "96%+ line coverage on stores, 95%+ on mappers" (REQUIREMENTS.md:52)

**Recommendation**: Clarify whether these are overall coverage vs per-package coverage.

---

## Low Priority Issues

### 11. Outdated ADR References

Some ADRs may reference old implementation details:
- ADR-0009 (test-strategy) may need updates for forkCount=1 and singleton test disabling
- ADR-0048 (testcontainers-single-container-lifecycle) may need updates for @ServiceConnection-only approach

**Recommendation**: Review and update test-related ADRs.

### 12. Missing Changelog Entry

Recent changes (SpaCsrfTokenRequestHandler, frontend fixes, forkCount=1, singleton test disabling) are not in `docs/logs/CHANGELOG.md`

**Recommendation**: Add changelog entry for 2025-12-02 changes.

---

## Recommended Actions

### Immediate (Before Next Commit)

1. **Count actual tests** - Run `mvn test` and get authoritative count
2. **Run PITest** - Get authoritative mutation score
3. **Update all docs** with consistent numbers
4. **Add SpaCsrfTokenRequestHandler documentation** to Security section
5. **Clarify singleton test status** - which are disabled and why

### Short Term (This Week)

6. **Document frontend fixes** in React UI section
7. **Add Test Execution Strategy section** explaining forkCount=1 and its implications
8. **Update ROADMAP.md** to show Phase 6 complete
9. **Add TestCleanupUtility documentation**
10. **Update CHANGELOG.md** with recent changes

### Medium Term (Next Sprint)

11. **Review and update test-related ADRs** (ADR-0009, ADR-0048)
12. **Document @ServiceConnection migration** from @DynamicPropertySource
13. **Audit UI component documentation** (out of scope for this audit)

---

## Files Requiring Updates

### Priority 1 (Critical)
- [ ] `README.md` - Test counts, mutation scores, Security section, Test Execution section
- [ ] `docs/REQUIREMENTS.md` - Test counts, mutation scores, Phase 5 security details
- [ ] `docs/ROADMAP.md` - Phase 6 status, test counts
- [ ] `PROJECT_SUMMARY.md` - Test counts, mutation scores

### Priority 2 (Important)
- [ ] `docs/logs/CHANGELOG.md` - Add 2025-12-02 entries
- [ ] `docs/adrs/ADR-0009-test-strategy.md` - forkCount=1 implications
- [ ] `docs/adrs/ADR-0048-testcontainers-single-container-lifecycle.md` - @ServiceConnection approach

### Priority 3 (Nice to Have)
- [ ] `docs/INDEX.md` - Verify SpaCsrfTokenRequestHandler is listed
- [ ] `docs/architecture/threat-model.md` - CSRF protection updates if needed

---

## Verification Checklist

After making updates, verify:
- [ ] All test count references use same number
- [ ] All mutation score references use same percentage
- [ ] SpaCsrfTokenRequestHandler mentioned in Security section of README, REQUIREMENTS, and PROJECT_SUMMARY
- [ ] Frontend fixes (noValidate, empty SelectItem) documented in React UI section
- [ ] Test execution strategy clearly explains forkCount=1, singleton persistence, and @Disabled tests
- [ ] ROADMAP.md shows Phase 6 as complete, not deferred
- [ ] TestCleanupUtility explained in test strategy
- [ ] CHANGELOG.md has entry for 2025-12-02 changes

---

## Notes

- This audit focused on main documentation files (README, REQUIREMENTS, ROADMAP, PROJECT_SUMMARY)
- ADR-level audit was not completed due to scope
- UI-specific documentation (`ui/contact-app/README.md`) was not audited
- API documentation (OpenAPI/Swagger) was not audited

**Audit Duration**: Approximately 30 minutes
**Files Examined**: 4 main documentation files, multiple test files, pom.xml, security configuration
**Total Issues Found**: 12 (3 critical, 4 high impact, 3 medium, 2 low)
