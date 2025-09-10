# coc-java

[![Publish Javadoc](https://github.com/ChangeinX/coc.py/actions/workflows/publish-javadoc.yml/badge.svg?branch=master)](https://github.com/ChangeinX/coc.py/actions/workflows/publish-javadoc.yml)
[![Javadocs](https://img.shields.io/badge/docs-Javadoc-blue)](https://ChangeinX.github.io/coc.py/)

Java library (in-progress) for Clash of Clans API.

This module starts the migration from the legacy Python client to Java with TDD. It currently supports:

- Login via developer.clashofclans.com using `DevSiteAuthenticator`
- Multi-token acquisition and round-robin token rotation
- Simple sliding-window throttling across all tokens
- Fetch clan info: `getClan(tag)`
- Search clans: `searchClans(name, limit)`
- List clan members: `getMembers(clanTag, limit, after, before)`
- Players: `getPlayer(tag)`, `verifyPlayerToken(tag, token)`
- Labels: `getClanLabels()`, `getPlayerLabels()`
- Locations: `searchLocations(limit)`, `getLocation(id)`
- Wars: `getWarLog(clanTag, limit)`, `getCurrentWar(clanTag)`
- CWL: `getClanWarLeagueGroup(clanTag)`, `getCwlWar(warTag)`
- Rankings: `getLocationClanRankings(...)`, `getLocationPlayerRankings(...)`,
  `getLocationBuilderBaseClanRankings(...)`, `getLocationBuilderBasePlayerRankings(...)`, `getLocationCapitalClanRankings(...)`

## Build & Test

- Run tests: `./gradlew :coc-java:test`
- Build: `./gradlew :coc-java:build`

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

// Or request multiple tokens and set per-token rate
client.login(System.getenv("COC_DEV_EMAIL"), System.getenv("COC_DEV_PASSWORD"), 3, 30);
```

## Example Runner

Set env vars `COC_DEV_EMAIL` and `COC_DEV_PASSWORD`, then:

```
./gradlew :coc-java:runLoginExample --args '#2PP'
```

## Design Notes

- HTTP layer is pluggable via `HttpTransport`; tests inject fakes to avoid network.
- `DevSiteAuthenticator` mirrors Python logic: login, derive IP from temporary token, list keys, revoke mismatched, create as needed.
- `TokenRotator` cycles tokens per request; `RateLimiter` applies a sliding window across all tokens.
- Tag normalization mirrors Python (`correct_tag`): uppercase, `O`→`0`, remove non-alnum, ensure leading `#`, and encodes `#` as `%23` in URLs.

### Documentation

- Browse Javadocs: https://changeinx.github.io/coc.py/
- Direct overview: https://changeinx.github.io/coc.py/overview-summary.html

### Docs Publishing (GitHub Pages)

- Workflow: `.github/workflows/publish-javadoc.yml`.
- Triggers: pushes to `master`/`main` (for changes under `coc-java/**`, workflow file, or Gradle files), and manual dispatch.
- Build: runs `./gradlew :coc-java:javadoc` to generate docs in `coc-java/build/docs/javadoc`.
- Publish: deploys to branch `gh-pages`, folder `docs/` using `peaceiris/actions-gh-pages`.
  - Adds `.nojekyll` to avoid Jekyll filtering.
  - Adds `index.htm` that redirects to `index.html` for compatibility.
- GitHub Pages settings: Source `gh-pages`, Folder `/docs`.
- Live docs URL: https://changeinx.github.io/coc.py/
- Manual run: Actions → "Publish Javadoc" → Run workflow.
- Troubleshooting:
  - 404 after deploy: verify Pages settings (branch `gh-pages`, path `/docs`) and that `docs/index.html` exists on `gh-pages`.
  - Pages build “errored”: re-run CI; ensure `.nojekyll` exists in `docs/`; allow up to ~10 minutes for CDN cache.

### Behavior Notes

- `verifyPlayerToken(tag, token)`
  - POSTs to `/players/{tag}/verifytoken` with JSON body `{ "token": "..." }`.
  - Returns `true` when response JSON has `status` equal to `ok` (case-insensitive); returns `false` for other 2xx bodies or malformed JSON.
  - Throws `NotFoundException` on 404 when the player tag does not exist.
  - Throws `RuntimeException` for other non-2xx responses, mirroring error handling of other endpoints.
