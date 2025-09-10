# coc-java

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
