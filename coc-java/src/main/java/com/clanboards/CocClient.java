package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.exceptions.NotFoundException;
import com.clanboards.exceptions.PrivateWarLogException;
import com.clanboards.http.HttpRequest;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import com.clanboards.throttle.RateLimiter;
import com.clanboards.token.TokenRotator;
import com.clanboards.util.TagUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Java client for the Clash of Clans API (initial scaffolding).
 * Supports login with email/password (via injected Authenticator) and getClan(tag).
 */
public class CocClient {
    private final HttpTransport transport;
    private final Authenticator authenticator;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    private volatile TokenRotator tokenRotator;
    private volatile RateLimiter rateLimiter; // total limit across tokens per window

    public CocClient(HttpTransport transport, Authenticator authenticator) {
        this(transport, authenticator, "https://api.clashofclans.com/v1");
    }

    public CocClient(HttpTransport transport, Authenticator authenticator, String baseUrl) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    }

    /** Obtain one token with default throttling (10 req/sec per token). */
    public void login(String email, String password) {
        login(email, password, 1, 10);
    }

    /** Obtain multiple tokens and configure throttling (per-token requests per second). */
    public void login(String email, String password, int tokenCount, int perTokenRate) {
        List<String> tokens = authenticator.obtainTokens(email, password, tokenCount);
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalStateException("Authenticator returned no tokens");
        }
        this.tokenRotator = new TokenRotator(tokens);
        int totalRate = Math.max(1, tokens.size() * Math.max(1, perTokenRate));
        this.rateLimiter = new RateLimiter(totalRate, 1000);
    }

    /** Use provided tokens and configure throttling. */
    public void loginWithTokens(List<String> tokens, int perTokenRate) {
        if (tokens == null || tokens.isEmpty()) throw new IllegalArgumentException("tokens empty");
        this.tokenRotator = new TokenRotator(tokens);
        int totalRate = Math.max(1, tokens.size() * Math.max(1, perTokenRate));
        this.rateLimiter = new RateLimiter(totalRate, 1000);
    }

    /** Get information about a single clan by tag. */
    public Clan getClan(String tag) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");

        String corrected = TagUtil.correctTag(tag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/clans/" + encoded;

        // throttle globally across tokens
        if (rateLimiter != null) rateLimiter.acquire();

        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();

        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 404) {
            throw new NotFoundException("Clan not found: " + corrected);
        }
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling getClan: " + body);
        }
        try {
            return mapper.readValue(resp.getBody(), Clan.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse clan JSON", e);
        }
    }

    /** Search clans by name (and optional limit). At least one filter must be provided. */
    public java.util.List<Clan> searchClans(String name, Integer limit) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        if ((name == null || name.isBlank()) && (limit == null || limit <= 0)) {
            throw new IllegalArgumentException("At least one filter (e.g., name) must be provided");
        }
        StringBuilder url = new StringBuilder(baseUrl).append("/clans");
        String sep = "?";
        if (name != null && !name.isBlank()) {
            url.append(sep).append("name=").append(encode(name));
            sep = "&";
        }
        if (limit != null && limit > 0) {
            url.append(sep).append("limit=").append(limit);
        }

        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url.toString())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();

        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling searchClans: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<Clan> result = new java.util.ArrayList<>();
            if (items.isArray()) {
                for (var it : items) {
                    result.add(mapper.convertValue(it, Clan.class));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse searchClans JSON", e);
        }
    }

    /** List clan members. Optional: limit, after, before. */
    public java.util.List<ClanMember> getMembers(String clanTag, Integer limit, String after, String before) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");

        String corrected = TagUtil.correctTag(clanTag);
        String encoded = TagUtil.encodeForPath(corrected);
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/clans/")
                .append(encoded)
                .append("/members");
        String sep = "?";
        if (limit != null && limit > 0) { url.append(sep).append("limit=").append(limit); sep = "&"; }
        if (after != null && !after.isBlank()) { url.append(sep).append("after=").append(encode(after)); sep = "&"; }
        if (before != null && !before.isBlank()) { url.append(sep).append("before=").append(encode(before)); }

        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url.toString())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();

        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 404) {
            throw new NotFoundException("Clan not found: " + corrected);
        }
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling getMembers: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<ClanMember> result = new java.util.ArrayList<>();
            if (items.isArray()) {
                for (var it : items) {
                    result.add(mapper.convertValue(it, ClanMember.class));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse getMembers JSON", e);
        }
    }

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Players
    public Player getPlayer(String tag) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String corrected = TagUtil.correctTag(tag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/players/" + encoded;
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() == 404) throw new NotFoundException("Player not found: " + corrected);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling getPlayer: " + body);
        }
        try {
            return mapper.readValue(resp.getBody(), Player.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Player JSON", e);
        }
    }

    public boolean verifyPlayerToken(String tag, String tokenToVerify) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String corrected = TagUtil.correctTag(tag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/players/" + encoded + "/verifytoken";
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        byte[] body = writeJson(java.util.Map.of("token", tokenToVerify));
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.POST)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) return false;
        try {
            var node = mapper.readTree(resp.getBody());
            String status = node.path("status").asText("");
            return "ok".equalsIgnoreCase(status);
        } catch (Exception e) {
            return false;
        }
    }

    // Labels
    public java.util.List<Label> getClanLabels() {
        return getLabels("/labels/clan");
    }
    public java.util.List<Label> getPlayerLabels() {
        return getLabels("/labels/players");
    }
    private java.util.List<Label> getLabels(String path) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling " + path + ": " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<Label> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, Label.class));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse labels JSON", e);
        }
    }

    // Locations
    public java.util.List<Location> searchLocations(Integer limit) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = baseUrl + "/locations" + (limit != null && limit > 0 ? ("?limit=" + limit) : "");
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling searchLocations: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<Location> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, Location.class));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse locations JSON", e);
        }
    }

    public Location getLocation(int id) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = baseUrl + "/locations/" + id;
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() == 404) throw new NotFoundException("Location not found: " + id);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling getLocation: " + body);
        }
        try {
            return mapper.readValue(resp.getBody(), Location.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse location JSON", e);
        }
    }

    private byte[] writeJson(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Rankings
    public java.util.List<RankedClan> getLocationClanRankings(int locationId, Integer limit, String after, String before) {
        return getRankedClans("/locations/" + locationId + "/rankings/clans", limit, after, before);
    }

    public java.util.List<RankedClan> getLocationBuilderBaseClanRankings(int locationId, Integer limit, String after, String before) {
        return getRankedClans("/locations/" + locationId + "/rankings/clans-builder-base", limit, after, before);
    }

    public java.util.List<RankedClan> getLocationCapitalClanRankings(int locationId, Integer limit, String after, String before) {
        return getRankedClans("/locations/" + locationId + "/rankings/capitals", limit, after, before);
    }

    public java.util.List<RankedPlayer> getLocationPlayerRankings(int locationId, Integer limit, String after, String before) {
        return getRankedPlayers("/locations/" + locationId + "/rankings/players", limit, after, before);
    }

    public java.util.List<RankedPlayer> getLocationBuilderBasePlayerRankings(int locationId, Integer limit, String after, String before) {
        return getRankedPlayers("/locations/" + locationId + "/rankings/players-builder-base", limit, after, before);
    }

    private java.util.List<RankedClan> getRankedClans(String path, Integer limit, String after, String before) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = buildUrlWithPaging(baseUrl + path, limit, after, before);
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 404) throw new NotFoundException("Location or resource not found");
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling rankings: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<RankedClan> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, RankedClan.class));
            return result;
        } catch (Exception e) { throw new RuntimeException("Failed to parse ranked clans JSON", e); }
    }

    private java.util.List<RankedPlayer> getRankedPlayers(String path, Integer limit, String after, String before) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = buildUrlWithPaging(baseUrl + path, limit, after, before);
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 404) throw new NotFoundException("Location or resource not found");
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling rankings: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<RankedPlayer> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, RankedPlayer.class));
            return result;
        } catch (Exception e) { throw new RuntimeException("Failed to parse ranked players JSON", e); }
    }

    private static String buildUrlWithPaging(String base, Integer limit, String after, String before) {
        StringBuilder sb = new StringBuilder(base);
        String sep = base.contains("?") ? "&" : "?";
        if (limit != null && limit > 0) { sb.append(sep).append("limit=").append(limit); sep = "&"; }
        if (after != null && !after.isBlank()) { sb.append(sep).append("after=").append(encode(after)); sep = "&"; }
        if (before != null && !before.isBlank()) { sb.append(sep).append("before=").append(encode(before)); }
        return sb.toString();
    }

    // Leagues
    public java.util.List<League> searchLeagues(Integer limit) { return getLeagues("/leagues", limit); }
    public League getLeague(int leagueId) { return getLeagueByPath("/leagues/" + leagueId); }

    public java.util.List<League> getCapitalLeagues(Integer limit) { return getLeagues("/capitalleagues", limit); }
    public League getCapitalLeague(int id) { return getLeagueByPath("/capitalleagues/" + id); }

    public java.util.List<League> getWarLeagues(Integer limit) { return getLeagues("/warleagues", limit); }
    public League getWarLeague(int id) { return getLeagueByPath("/warleagues/" + id); }

    public java.util.List<League> getBuilderBaseLeagues(Integer limit) { return getLeagues("/builderbaseleagues", limit); }
    public League getBuilderBaseLeague(int id) { return getLeagueByPath("/builderbaseleagues/" + id); }

    public java.util.List<SeasonRef> getLeagueSeasons(int leagueId, Integer limit, String after, String before) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = buildUrlWithPaging(baseUrl + "/leagues/" + leagueId + "/seasons", limit, after, before);
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling getLeagueSeasons: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<SeasonRef> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, SeasonRef.class));
            return result;
        } catch (Exception e) { throw new RuntimeException("Failed to parse seasons JSON", e); }
    }

    public java.util.List<RankedPlayer> getLeagueSeasonInfo(int leagueId, String seasonId, Integer limit, String after, String before) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = buildUrlWithPaging(baseUrl + "/leagues/" + leagueId + "/seasons/" + encode(seasonId), limit, after, before);
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling getLeagueSeasonInfo: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<RankedPlayer> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, RankedPlayer.class));
            return result;
        } catch (Exception e) { throw new RuntimeException("Failed to parse season info JSON", e); }
    }

    private java.util.List<League> getLeagues(String path, Integer limit) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = buildUrlWithPaging(baseUrl + path, limit, null, null);
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling " + path + ": " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<League> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, League.class));
            return result;
        } catch (Exception e) { throw new RuntimeException("Failed to parse leagues JSON", e); }
    }

    private League getLeagueByPath(String path) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = baseUrl + path;
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() == 404) throw new NotFoundException("League not found");
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling " + path + ": " + body);
        }
        try { return mapper.readValue(resp.getBody(), League.class);} catch (Exception e) { throw new RuntimeException(e);}        
    }

    // Gold Pass
    public GoldPassSeason getCurrentGoldPassSeason() {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String url = baseUrl + "/goldpass/seasons/current";
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling getCurrentGoldPassSeason: " + body);
        }
        try { return mapper.readValue(resp.getBody(), GoldPassSeason.class);} catch (Exception e) { throw new RuntimeException(e);}        
    }

    // War endpoints
    public java.util.List<com.clanboards.wars.ClanWarLogEntry> getWarLog(String clanTag, Integer limit) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String corrected = TagUtil.correctTag(clanTag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/clans/" + encoded + "/warlog" + (limit != null && limit > 0 ? ("?limit=" + limit) : "");
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 403) throw new PrivateWarLogException("Clan war log is private: " + corrected);
        if (sc == 404) throw new NotFoundException("Clan not found: " + corrected);
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling getWarLog: " + body);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            var items = node.path("items");
            java.util.List<com.clanboards.wars.ClanWarLogEntry> result = new java.util.ArrayList<>();
            if (items.isArray()) for (var it : items) result.add(mapper.convertValue(it, com.clanboards.wars.ClanWarLogEntry.class));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse war log JSON", e);
        }
    }

    public com.clanboards.wars.ClanWar getCurrentWar(String clanTag) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String corrected = TagUtil.correctTag(clanTag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/clans/" + encoded + "/currentwar";
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 403) throw new PrivateWarLogException("Clan war log is private: " + corrected);
        if (sc == 404) throw new NotFoundException("Clan not found or no current war: " + corrected);
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling getCurrentWar: " + body);
        }
        try {
            return mapper.readValue(resp.getBody(), com.clanboards.wars.ClanWar.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse current war JSON", e);
        }
    }

    // CWL endpoints
    public com.clanboards.wars.ClanWarLeagueGroup getClanWarLeagueGroup(String clanTag) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String corrected = TagUtil.correctTag(clanTag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/clans/" + encoded + "/currentwar/leaguegroup";
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 403) throw new PrivateWarLogException("Clan war league group is private: " + corrected);
        if (sc == 404) throw new NotFoundException("Clan not found: " + corrected);
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling getClanWarLeagueGroup: " + body);
        }
        try {
            // The API returns rounds as array of objects with warTags; we transform to List<List<String>> with filtering
            var root = mapper.readTree(resp.getBody());
            var group = new com.clanboards.wars.ClanWarLeagueGroup();
            if (root.hasNonNull("state")) group.setState(root.get("state").asText());
            if (root.hasNonNull("season")) group.setSeason(root.get("season").asText());
            var roundsNode = root.path("rounds");
            java.util.List<java.util.List<String>> rounds = new java.util.ArrayList<>();
            if (roundsNode.isArray()) {
                for (var r : roundsNode) {
                    var warTagsNode = r.path("warTags");
                    java.util.List<String> warTags = new java.util.ArrayList<>();
                    if (warTagsNode.isArray()) {
                        for (var wt : warTagsNode) warTags.add(wt.asText());
                    }
                    rounds.add(warTags);
                }
            }
            group.setNumberOfRounds(rounds.size());
            group.setRounds(rounds); // filters out #0
            var clansNode = root.path("clans");
            java.util.List<com.clanboards.wars.CwlClan> clans = new java.util.ArrayList<>();
            if (clansNode.isArray()) {
                for (var c : clansNode) clans.add(mapper.convertValue(c, com.clanboards.wars.CwlClan.class));
            }
            group.setClans(clans);
            return group;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse league group JSON", e);
        }
    }

    public com.clanboards.wars.ClanWar getCwlWar(String warTag) {
        if (tokenRotator == null) throw new IllegalStateException("Client not logged in");
        String corrected = TagUtil.correctTag(warTag);
        String encoded = TagUtil.encodeForPath(corrected);
        String url = baseUrl + "/clanwarleagues/wars/" + encoded;
        if (rateLimiter != null) rateLimiter.acquire();
        String token = tokenRotator.next();
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.GET)
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse resp = transport.execute(req);
        int sc = resp.getStatusCode();
        if (sc == 403) throw new PrivateWarLogException("League war is private or forbidden: " + corrected);
        if (sc == 404) throw new NotFoundException("League war not found: " + corrected);
        if (sc < 200 || sc >= 300) {
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + sc + " calling getCwlWar: " + body);
        }
        try {
            return mapper.readValue(resp.getBody(), com.clanboards.wars.ClanWar.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CWL war JSON", e);
        }
    }

    /** Expose first token (read-only) for diagnostics. */
    public String getToken() { return tokenRotator != null ? tokenRotator.getAll().get(0) : null; }
}
