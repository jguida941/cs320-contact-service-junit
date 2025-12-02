# ADR-0029: CI as Quality Gate Philosophy

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

## Context
- As a junior developer, I needed confidence that changes wouldn't break existing functionality.
- Manual testing is error-prone and inconsistent.
- I wanted strict enforcement that would catch issues before they reached the main branch.

## Decision
- Treat CI/CD as a strict quality gate, not just automation.
- The pipeline must pass before any code merges - no exceptions.
- Quality checks include: compilation, unit tests, mutation testing (PITest), static analysis (SpotBugs), code style (Checkstyle), and dependency vulnerability scanning (OWASP).
- If CI fails, the code is not ready - regardless of whether "it works on my machine."

## Consequences
- High confidence in code quality; broken builds are caught immediately.
- Slower iteration when CI fails, but prevents accumulating technical debt.
- Forces discipline: can't skip tests or ignore warnings.
- Acts as a safety net when using AI assistance - AI-generated code must pass the same gates as manual code.

## Why This Matters
As someone learning while building, CI gates provided guardrails. I could experiment knowing the pipeline would catch mistakes. This is especially valuable when using AI tools - the AI can generate code, but CI validates it actually works.
