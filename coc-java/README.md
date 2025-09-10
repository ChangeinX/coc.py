# coc-java

[![Publish Javadoc](https://github.com/ChangeinX/coc.py/actions/workflows/publish-javadoc.yml/badge.svg?branch=master)](https://github.com/ChangeinX/coc.py/actions/workflows/publish-javadoc.yml)
[![Javadocs](https://img.shields.io/badge/docs-Javadoc-blue)](https://ChangeinX.github.io/coc.py/)

Concise Java client for the Clash of Clans API. This module is the new primary implementation and follows strict TDD.

Core features (parity with Python where applicable):

- Auth via developer.clashofclans.com (`DevSiteAuthenticator`)
- Multi-token acquisition + round‑robin rotation
- Sliding‑window rate limiting across all tokens
- Clans: `getClan(tag)`, `searchClans(name, limit)`, `getMembers(tag, ...)`
- Players: `getPlayer(tag)`, `verifyPlayerToken(tag, token)`
- Labels: `getClanLabels()`, `getPlayerLabels()`
- Locations: `searchLocations(limit)`, `getLocation(id)`
- Wars: `getWarLog(tag, limit)`, `getCurrentWar(tag)`
- CWL: `getClanWarLeagueGroup(tag)`, `getCwlWar(warTag)`
- Rankings: location clan/player, builder base, capital rankings

## Quick Start

```java
import com.clanboards.CocClient;
import com.clanboards.Clan;
import com.clanboards.auth.DevSiteAuthenticator;
import com.clanboards.http.DefaultHttpTransport;

var transport = new DefaultHttpTransport();
var authenticator = new DevSiteAuthenticator(transport);
var client = new CocClient(transport, authenticator);

// Single token with default throttling (10 req/sec/token)
client.login(System.getenv("COC_DEV_EMAIL"), System.getenv("COC_DEV_PASSWORD"));
Clan clan = client.getClan("#2PP");

// Or request multiple tokens and set per‑token rate
client.login(System.getenv("COC_DEV_EMAIL"), System.getenv("COC_DEV_PASSWORD"), 3, 30);
```

## Build & Test

- Run tests: `./gradlew :coc-java:test`
- Build: `./gradlew :coc-java:build`
- Generate docs: `./gradlew :coc-java:javadoc`

## Module Layout

- Source: `coc-java/src/main/java/com/clanboards/**`
- Tests: `coc-java/src/test/java/com/clanboards/**`
- Test fixtures: `coc-java/src/test/resources/**`
- Example: `com.clanboards.examples.LoginExample`

## API Cheatsheet

- Clans: `getClan(tag)`, `searchClans(name, limit)`, `getMembers(tag, limit, after, before)`
- Players: `getPlayer(tag)`, `verifyPlayerToken(tag, token)`
- Labels: `getClanLabels()`, `getPlayerLabels()`
- Locations: `searchLocations(limit)`, `getLocation(id)`
- Wars: `getWarLog(clanTag, limit)`, `getCurrentWar(clanTag)`
- CWL: `getClanWarLeagueGroup(clanTag)`, `getCwlWar(warTag)`
- Rankings: `getLocationClanRankings`, `getLocationPlayerRankings`, `getLocationBuilderBaseClanRankings`, `getLocationBuilderBasePlayerRankings`, `getLocationCapitalClanRankings`

## Error Semantics

- 404 → `NotFoundException`
- Private war log (403) → `PrivateWarLogException`
- Other non‑2xx → `RuntimeException` (or subtypes as added)
- `verifyPlayerToken` returns `true` iff JSON `status` equals `ok` (case‑insensitive) on 2xx; otherwise `false`

## Tokens & Throttling

- Tokens: Managed by `DevSiteAuthenticator` using the dev site API (list/revoke/create).
- Rotation: `TokenRotator` cycles tokens per request.
- Rate limiting: `RateLimiter` applies a sliding window across all tokens. Default is 10 req/sec/token; override via `login(email, pass, tokenCount, perTokenRate)` or `loginWithTokens(tokens, perTokenRate)`.

## Contributing (TDD)

- Write a failing JUnit 5 test first under `src/test/java`.
- Use hermetic tests: inject a fake `HttpTransport` and feed JSON from `src/test/resources`.
- Implement the smallest change in `src/main/java` to pass tests.
- Run `./gradlew :coc-java:test` until green; then refactor safely.

Test pattern example (pseudo‑code):

```java
HttpTransport transport = (req) -> HttpResponse.ok(json("players/FOUND.json"));
var client = new CocClient(transport, authenticator);
assertEquals("#TAG", client.getPlayer("#tag").getTag());
```

## Documentation

- Javadoc: https://changeinx.github.io/coc.py/
- Overview: https://changeinx.github.io/coc.py/overview-summary.html

### Docs Publishing (GitHub Pages)

- Workflow: `.github/workflows/publish-javadoc.yml`
- Triggers: push to `master`/`main` (changes in `coc-java/**` or Gradle/workflow files) and manual dispatch
- Build: `./gradlew :coc-java:javadoc` → `coc-java/build/docs/javadoc`
- Publish: CI deploys to `gh-pages` branch under `docs/`
  - Includes `.nojekyll` and an `index.htm` redirect for compatibility
- Pages settings: Source `gh-pages`, Folder `/docs`
  - Live URL: https://changeinx.github.io/coc.py/

## Notes

- HTTP layer is pluggable via `HttpTransport`.
- Tag normalization mirrors Python (`correct_tag`): uppercase, `O→0`, strip non‑alnum, ensure leading `#`, URL‑encode as `%23`.
