## Contributing to coc-java

This module is developed with strict TDD.

- Write a failing JUnit 5 test first under `src/test/java`.
- Keep tests hermetic: inject a fake `HttpTransport` and use JSON fixtures in `src/test/resources`.
- Implement the minimal code in `src/main/java` to pass tests.
- Run `./gradlew :coc-java:test` locally until green.
- Add Javadoc on public types/methods; update package docs if adding packages.

See repo-wide guidelines in `AGENTS.md` and `MIGRATION.md` for context and scope.

