# SpotBugs runtime support and JDK issues

(Related: [ADR-0005](../../adrs/ADR-0005-spotbugs-runtime-support.md), [pom.xml](../../pom.xml))

File: docs/design-notes/notes/spotbugs-notes.md

## What problem this solves
- SpotBugs sometimes breaks when new JDK releases come out (bytecode format changes, etc.).
- Without a plan, developers might disable SpotBugs entirely, leaving us without static-analysis coverage.
- SpotBugs works on the compiled bytecode (the `.class` files Java produces).  
  It runs checks like:
  ```java
  // Null dereference example
  String formatName(Contact obj) {
      return obj.toString(); // If obj is null, this line throws NullPointerException
  }
  // SpotBugs warns because callers might pass null, so we should check first or require non-null.
  ```
  ```java
  // Dodgy equals/hashCode example
  class Contact {
      private final String id;
      @Override
      public boolean equals(Object other) { ... }
      // hashCode missing -> HashMap/HashSet behavior breaks, SpotBugs flags it
  }
  ```
  // SpotBugs warns because collections rely on equals and hashCode staying in sync.
  ```java
  // Concurrency example
  class Counters {
      private static int visits = 0;
      void increment() { visits++; } // SpotBugs warns: incrementing shared static field without synchronization
  }
  ```
  Losing SpotBugs would remove these automated checks entirely.

## What the design is
- Pin `spotbugs-maven-plugin` to a known good version (e.g., 4.9.7.0) and run it during `mvn verify`.
- If a new JDK breaks SpotBugs locally, developers can temporarily run it on JDK 17 while waiting for upstream fixes instead of disabling it outright.
- Keep `spotbugs.skip=false` by default so CI always runs the gate.

## Why this matters
- SpotBugs is part of the CI gates (static analysis). If it silently stops running, we lose a safety net for concurrency/null bugs.
- By pinning the plugin and documenting the workaround, we keep the gate reliable even when JDK versions evolve.
