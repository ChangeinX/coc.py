# Migration Progress (Python → Java)

This document tracks features ported from the legacy Python implementation to the new Java module `coc-java/`.
Only completed items are placed here, not a TODO list.

## Completed

- Client scaffolding (`CocClient`) with pluggable HTTP transport
  - `HttpTransport` interface + `DefaultHttpTransport` (Java 11 HttpClient)
- Authentication flow (`DevSiteAuthenticator`)
  - Login to developer site (`/api/login`)
  - Derive IP from `temporaryAPIToken`
  - List existing keys (`/api/apikey/list`)
  - Revoke mismatched named keys (`/api/apikey/revoke`) to free slots
  - Create new key(s) with current IP + scope (`/api/apikey/create`)
  - Returns one or more API tokens
- Multi-token rotation and throttling
  - Round-robin token use (`TokenRotator`) across requests
  - Sliding-window rate limiting across all tokens (`RateLimiter`)
  - `CocClient.login(email, password, tokenCount, perTokenRate)` to request N tokens and configure the rate
  - `CocClient.loginWithTokens(tokens, perTokenRate)` to supply custom tokens
- Basic data models and endpoints
  - `Clan` (minimal) and `getClan(tag)`
  - `ClanMember` and `getMembers(clanTag, limit, after, before)`
  - `searchClans(name, limit)`
  - `Player` and `getPlayer(tag)`, `verifyPlayerToken(tag, token)`
    - Parity with Python: 404 → `NotFoundException`, other non-2xx → `RuntimeException`,
      2xx with JSON `{"status":"ok"}` returns true (case-insensitive), otherwise false
  - Labels: `getClanLabels()`, `getPlayerLabels()`
  - Locations: `searchLocations(limit)`, `getLocation(id)`
  - Wars: `getWarLog(clanTag, limit)`, `getCurrentWar(clanTag)`
    - `PrivateWarLogException` thrown on 403 for private war logs
  - CWL: `getClanWarLeagueGroup(clanTag)`, `getCwlWar(warTag)`
  - Rankings: `getLocationClanRankings`, `getLocationPlayerRankings`, `getLocationBuilderBaseClanRankings`,
    `getLocationBuilderBasePlayerRankings`, `getLocationCapitalClanRankings`
  - Leagues: `searchLeagues(limit)`, `getLeague(id)`,
    `getCapitalLeagues(limit)`, `getCapitalLeague(id)`,
    `getWarLeagues(limit)`, `getWarLeague(id)`,
    `getBuilderBaseLeagues(limit)`, `getBuilderBaseLeague(id)`,
    `getLeagueSeasons(leagueId, ...)`, `getLeagueSeasonInfo(leagueId, seasonId, ...)`
  - Gold Pass: `getCurrentGoldPassSeason()`
  - Tag normalization and URL encoding equivalent to Python’s `correct_tag`
  - 404 mapped to `NotFoundException`
- TDD foundation
  - Hermetic unit tests for client, tag utilities, and authenticator behaviors

## How to Validate

- Unit tests: `./gradlew :coc-java:test`
- Live smoke test: `./gradlew :coc-java:runLoginExample --args '#2PP'` with `COC_DEV_EMAIL` and `COC_DEV_PASSWORD` set
