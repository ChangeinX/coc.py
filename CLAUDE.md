# Repository Guidelines

Note: This repository is migrating from Python to Java. Java is the new primary implementation, developed with strict Test‑Driven Development (TDD). Python code and tests remain as legacy until migration completes.

## Java Migration & TDD (New Primary)
- Module: `coc-java/` (Gradle, Groovy DSL). Use the standard layout: `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`.
- Base package: `com.clanboards`. Place code under `coc-java/src/main/java/com/clanboards` and tests under `coc-java/src/test/java/com/clanboards`.
- TDD policy for all Java work:
  - Write a failing test first (JUnit 5/Jupiter). Commit the test.
  - Implement the minimal code to pass the test.
  - Refactor safely with tests green. Repeat.
  - Keep unit tests fast and isolated; avoid network I/O. Use mocks/fakes (e.g., Mockito) and fixtures under `src/test/resources`.
  - Add tests for all new public behavior and important edge cases. Consider parameterized tests where useful.
  - If code coverage is configured (e.g., JaCoCo), keep or raise thresholds; do not lower them to pass CI.

## Project Structure & Module Organization
- `coc-java/` – Java library module (Gradle Groovy DSL).
  - Layout: `src/main/java`, `src/test/java`, `src/main/resources`, `src/test/resources`.
  - Base package path: `src/main/java/com/clanboards` (`com.clanboards`), tests mirror under `src/test/java/com/clanboards`.
  - Build files: `build.gradle` (Groovy), optional `gradle.properties`, and `settings.gradle` at root or module as applicable.
- `coc/` – legacy Python library source (core models, HTTP client, events, `ext/*`, and `static/` JSON assets).
- `tests/` – legacy Python unit tests using `unittest` and local `mockdata/` fixtures.
- `docs/` – Sphinx documentation; build artifacts are not checked in.
- `examples/` – runnable usage samples.
- Root files: Python packaging remains (`pyproject.toml`, `setup.py`, `README.rst`, `requirements.txt`). Gradle files will accompany the Java module.

## Build, Test, and Development Commands
- Java (Gradle, Groovy DSL):
  - Build: `./gradlew :coc-java:build`
  - Run tests: `./gradlew :coc-java:test`
  - Lint/format (if configured): `./gradlew check` (e.g., Checkstyle/Spotless), or the specific tasks defined in the build.
  - Coverage (if configured): `./gradlew :coc-java:jacocoTestReport`
  - Publish to GitHub Packages: `./gradlew :coc-java:publish` (see Publishing section for credentials setup).
- Python (legacy):
  - Create env and install: `python -m venv .venv && source .venv/bin/activate && pip install -e .`.
  - Docs: `pip install -r doc_requirements.txt && (cd docs && make html)`.
  - Run tests: `python -m unittest discover -s tests -p 'test_*.py'` (example: `python -m unittest tests/test_clans.py`).
  - Lint/format (if installed): `black coc tests`, `flake8 coc tests`, optional `pylint coc`.

## Coding Style & Naming Conventions
- Java:
  - Use standard Java conventions: classes `CapWords`, methods and fields `camelCase`, constants `UPPER_SNAKE`.
  - Prefer 4‑space indentation; no tabs for new code.
  - Keep public APIs well‑documented with clear Javadoc on exported types and methods.
  - Leverage generics and `Optional` where appropriate; avoid `null` where practical.
  - Follow any configured linters/formatters (e.g., Checkstyle, Spotless). Do not bypass checks.
  - Base package: `com.clanboards`. New code should live under this package unless there is a clear modular reason otherwise.
- Python (legacy):
  - Python 3.9+. Prefer 4‑space indentation and Black‑style formatting. Some legacy files use tabs; keep surrounding style when editing small sections, but use spaces for new code.
  - Naming: modules and functions `snake_case`; classes `CapWords`; constants `UPPER_SNAKE`.
  - Add type hints where practical; keep public API docstrings clear and user‑facing.
  - Do not modify `coc/static/` JSON unless the change is required by new game data and is referenced by code.

## Testing Guidelines
- Java (TDD required):
  - Framework: JUnit 5 (Jupiter). Use Mockito/AssertJ (if added) for mocking and fluent assertions.
  - Apply TDD: failing test → minimal implementation → refactor. Commit in small, reviewable steps.
  - Keep tests hermetic: no network I/O; mock HTTP; prefer deterministic fixtures.
  - Organize tests mirroring package structure under `src/test/java`; place fixture files in `src/test/resources`.
  - Run locally with `./gradlew :coc-java:test` (and coverage if configured) before opening a PR.
- Python (legacy):
  - Framework: `unittest`. Put tests in `tests/` as `test_*.py`; use fixtures in `tests/mockdata/`; avoid network I/O.
  - Run `python -m unittest -v` locally before opening a PR.

## Publishing (GitHub Packages)
- Target registry: `maven.pkg.github.com/ChangeinX/REPO`.
- Gradle (Groovy DSL) example for `coc-java/build.gradle`:

```
plugins {
  id 'java-library'
  id 'maven-publish'
}

java {
  // Optionally declare toolchain if desired
  // toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

publishing {
  publications {
    mavenJava(MavenPublication) {
      from components.java
      // Coordinates can also be configured in gradle.properties
      group = 'com.clanboards'
      artifactId = 'coc-java'
      // version may be supplied via CI or gradle.properties
    }
  }
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/ChangeinX/REPO")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user")
        password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key")
      }
    }
  }
}
```

- Environment variables expected for publish: set `GITHUB_ACTOR` and `GITHUB_TOKEN` (a PAT with `read:packages, write:packages` or the ephemeral Actions token in CI). Do not commit tokens.
- Publish command: `./gradlew :coc-java:publish`.
  - Coordinates: `com.clanboards:coc-java:<version>`.
  - Optionally set in `gradle.properties`: `group=com.clanboards` and `version=<version>`.

## Commit & Pull Request Guidelines
- Use Conventional Commit prefixes when possible: `feat:`, `fix:`, `docs:`, `chore:`, etc. Keep commits focused and descriptive.
- For Java changes: include or update tests in the same PR (TDD). CI must run `./gradlew build` successfully.
- PRs should include: concise description, motivation/context, linked issues, and tests/docs updates. Prefer small, reviewable PRs.

## Security & Configuration Tips
- Never commit credentials. Use environment variables (`GITHUB_ACTOR`, `GITHUB_TOKEN`) for publishing.
- Be mindful of API rate limits; for Java code, mock HTTP in tests and use throttling utilities if/when added. For Python legacy code, prefer built‑in throttlers in `coc.http` when adding HTTP behavior.

## Agent‑Specific Instructions
- Follow this guide repo‑wide. Keep changes minimal and scoped; do not refactor unrelated code.
- Java is primary: place new implementation work under `coc-java/` and practice TDD.
- Preserve APIs; add deprecations instead of breaking changes.
- Before pushing: for Java run `./gradlew build` (and coverage if configured); for Python, format, lint, and run the test suite. Update docs/examples if behavior changes.
