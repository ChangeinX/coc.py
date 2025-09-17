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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Primary client for interacting with the Clash of Clans API.
 *
 * This client provides comprehensive access to clan data, player information, rankings,
 * war logs, and league data. The client supports token rotation for high-throughput
 * applications and includes built-in rate limiting to respect API constraints.
 *
 * Thread-safety: This class is thread-safe after login completion. Multiple threads
 * can safely call API methods concurrently.
 *
 * @see <a href="https://developer.clashofclans.com/">Clash of Clans API Documentation</a>
 * @since 0.1.0
 */
public class CocClient {
    private final HttpTransport transport;
    private final Authenticator authenticator;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final boolean rawAttribute;

    private volatile TokenRotator tokenRotator;
    private volatile RateLimiter rateLimiter; // total limit across tokens per window

    /**
     * Creates a client with default API base URL and raw JSON disabled.
     *
     * @param transport HTTP transport implementation for making requests
     * @param authenticator authentication strategy for obtaining API tokens
     * @throws NullPointerException if transport or authenticator is null
     */
    public CocClient(HttpTransport transport, Authenticator authenticator) {
        this(transport, authenticator, "https://api.clashofclans.com/v1", false);
    }

    /**
     * Creates a client with default API base URL and configurable raw JSON support.
     *
     * @param transport HTTP transport implementation for making requests
     * @param authenticator authentication strategy for obtaining API tokens
     * @param rawAttribute whether to attach raw JSON data to response objects
     * @throws NullPointerException if transport or authenticator is null
     */
    public CocClient(HttpTransport transport, Authenticator authenticator, boolean rawAttribute) {
        this(transport, authenticator, "https://api.clashofclans.com/v1", rawAttribute);
    }

    /**
     * Creates a client with custom API base URL and raw JSON disabled.
     *
     * @param transport HTTP transport implementation for making requests
     * @param authenticator authentication strategy for obtaining API tokens
     * @param baseUrl custom base URL for API requests (useful for testing)
     * @throws NullPointerException if any parameter is null
     */
    public CocClient(HttpTransport transport, Authenticator authenticator, String baseUrl) {
        this(transport, authenticator, baseUrl, false);
    }

    /**
     * Primary constructor with full configuration options.
     *
     * @param transport HTTP transport implementation for making requests
     * @param authenticator authentication strategy for obtaining API tokens
     * @param baseUrl base URL for API requests
     * @param rawAttribute whether to attach raw JSON data to response objects
     * @throws NullPointerException if transport, authenticator, or baseUrl is null
     */
    public CocClient(HttpTransport transport, Authenticator authenticator, String baseUrl, boolean rawAttribute) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.rawAttribute = rawAttribute;
    }

    /**
     * Returns whether raw JSON attachment is enabled for response objects.
     *
     * When enabled, response objects will include the original JSON data
     * for debugging or extended processing purposes.
     *
     * @return true if raw JSON is attached to response objects
     */
    public boolean isRawAttributeEnabled() {
        return rawAttribute;
    }

    /**
     * Authenticates with a single API token using default rate limiting.
     *
     * Uses the configured authenticator to obtain one token and sets up
     * rate limiting at 10 requests per second.
     *
     * @param email developer account email
     * @param password developer account password
     * @throws IllegalStateException if authentication fails
     * @throws RuntimeException if authenticator encounters an error
     */
    public void login(String email, String password) {
        login(email, password, 1, 10);
    }

    /**
     * Authenticates with multiple API tokens and configures rate limiting.
     *
     * Uses the configured authenticator to obtain the specified number of tokens
     * and sets up rate limiting based on per-token request rates. Total rate limit
     * is calculated as tokenCount * perTokenRate.
     *
     * @param email developer account email
     * @param password developer account password
     * @param tokenCount number of tokens to obtain (must be > 0)
     * @param perTokenRate requests per second per token (must be > 0)
     * @throws IllegalStateException if authentication fails or no tokens obtained
     * @throws RuntimeException if authenticator encounters an error
     */
    public void login(String email, String password, int tokenCount, int perTokenRate) {
        List<String> tokens = authenticator.obtainTokens(email, password, tokenCount);
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalStateException("Authenticator returned no tokens");
        }
        this.tokenRotator = new TokenRotator(tokens);
        int totalRate = Math.max(1, tokens.size() * Math.max(1, perTokenRate));
        this.rateLimiter = new RateLimiter(totalRate, 1000);
    }

    /**
     * Configures the client with pre-obtained API tokens.
     *
     * Bypasses the authentication process by using provided tokens directly.
     * Useful when tokens are obtained through external means or cached.
     *
     * @param tokens list of valid API tokens (must not be null or empty)
     * @param perTokenRate requests per second per token (must be > 0)
     * @throws IllegalArgumentException if tokens is null or empty
     */
    public void loginWithTokens(List<String> tokens, int perTokenRate) {
        if (tokens == null || tokens.isEmpty()) throw new IllegalArgumentException("tokens empty");
        this.tokenRotator = new TokenRotator(tokens);
        int totalRate = Math.max(1, tokens.size() * Math.max(1, perTokenRate));
        this.rateLimiter = new RateLimiter(totalRate, 1000);
    }

    /**
     * Retrieves comprehensive information about a clan by its tag.
     *
     * The tag is automatically corrected to standard format (uppercase,
     * O->0 conversion, special character removal, # prefix addition).
     *
     * @param tag clan tag (e.g., "#2PP", "2pp", "2PP")
     * @return clan information including name, level, member count
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if clan with the specified tag does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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
            return decodeClan(resp.getBody());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse clan JSON", e);
        }
    }

    /**
     * Searches for clans matching the specified criteria.
     *
     * At least one filter parameter must be provided. The search is performed
     * against the clan name field and supports partial matching.
     *
     * @param name clan name to search for (partial matches supported)
     * @param limit maximum number of results to return (null for API default)
     * @return list of clans matching the search criteria
     * @throws IllegalStateException if client is not logged in
     * @throws IllegalArgumentException if no valid filter criteria provided
     * @throws RuntimeException if API request fails or response parsing fails
     */
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
                    result.add(convert(it, Clan.class));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse searchClans JSON", e);
        }
    }

    /**
     * Retrieves the member list for a specified clan.
     *
     * Supports pagination through before/after cursors for large clans.
     * The clan tag is automatically corrected to standard format.
     *
     * @param clanTag clan tag to get members for
     * @param limit maximum number of members to return (null for API default)
     * @param after cursor for pagination (members after this cursor)
     * @param before cursor for pagination (members before this cursor)
     * @return list of clan members with basic profile information
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if clan with the specified tag does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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
                    result.add(convert(it, ClanMember.class));
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

    private Clan decodeClan(byte[] body) throws IOException {
        if (!rawAttribute) {
            return mapper.readValue(body, Clan.class);
        }
        JsonNode node = mapper.readTree(body);
        Clan clan = mapper.treeToValue(node, Clan.class);
        attachRawJson(clan, node);
        return clan;
    }

    private <T> T convert(JsonNode node, Class<T> type) {
        T value = mapper.convertValue(node, type);
        attachRawJson(value, node);
        return value;
    }

    private void attachRawJson(Object value, JsonNode raw) {
        if (!rawAttribute || value == null || raw == null || raw.isMissingNode()) {
            return;
        }
        if (value instanceof Clan clan) {
            clan.attachRawJson(raw);
        } else if (value instanceof ClanMember member) {
            member.attachRawJson(raw);
        } else if (value instanceof RankedClan rankedClan) {
            rankedClan.attachRawJson(raw);
        }
    }

    /**
     * Retrieves comprehensive information about a player by their tag.
     *
     * The player tag is automatically corrected to standard format.
     * Returns detailed player statistics including town hall level, trophies,
     * and other game progression data.
     *
     * @param tag player tag (e.g., "#2PP", "2pp", "2PP")
     * @return player information and statistics
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if player with the specified tag does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Verifies a player's API token for authentication purposes.
     *
     * This endpoint allows verification of player-generated tokens
     * for secure API access and player identity confirmation.
     *
     * @param tag player tag to verify token for
     * @param tokenToVerify the token provided by the player to verify
     * @return true if the token is valid for the specified player
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if player with the specified tag does not exist
     * @throws RuntimeException if API request fails
     */
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
        if (resp.getStatusCode() == 404) throw new NotFoundException("Player not found: " + corrected);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String respBody = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " + resp.getStatusCode() + " calling verifyPlayerToken: " + respBody);
        }
        try {
            var node = mapper.readTree(resp.getBody());
            String status = node.path("status").asText("");
            return "ok".equalsIgnoreCase(status);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves all available clan labels from the API.
     *
     * Labels are used to categorize and filter clans based on various criteria
     * such as clan type, requirements, or focus areas.
     *
     * @return list of all available clan labels
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<Label> getClanLabels() {
        return getLabels("/labels/clan");
    }

    /**
     * Retrieves all available player labels from the API.
     *
     * Labels help categorize players and can be used for filtering
     * or organizational purposes.
     *
     * @return list of all available player labels
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves all available locations (countries and global) from the API.
     *
     * Locations are used for regional rankings and leaderboards.
     * Includes both country-specific locations and the global location.
     *
     * @param limit maximum number of locations to return (null for no limit)
     * @return list of available locations with country codes and names
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves information about a specific location by its ID.
     *
     * @param id location identifier
     * @return location details including name and country information
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if location with the specified ID does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves clan rankings for a specific location (main village).
     *
     * Returns clans ranked by their main village trophy count within
     * the specified geographical location. Supports pagination.
     *
     * @param locationId location identifier for rankings
     * @param limit maximum number of ranked clans to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of ranked clans with position and points information
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if location does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<RankedClan> getLocationClanRankings(int locationId, Integer limit, String after, String before) {
        return getRankedClans("/locations/" + locationId + "/rankings/clans", limit, after, before);
    }

    /**
     * Retrieves builder base clan rankings for a specific location.
     *
     * Returns clans ranked by their builder base trophy count within
     * the specified geographical location. Supports pagination.
     *
     * @param locationId location identifier for rankings
     * @param limit maximum number of ranked clans to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of ranked clans with builder base rankings
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if location does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<RankedClan> getLocationBuilderBaseClanRankings(int locationId, Integer limit, String after, String before) {
        return getRankedClans("/locations/" + locationId + "/rankings/clans-builder-base", limit, after, before);
    }

    /**
     * Retrieves clan capital rankings for a specific location.
     *
     * Returns clans ranked by their clan capital trophy count within
     * the specified geographical location. Supports pagination.
     *
     * @param locationId location identifier for rankings
     * @param limit maximum number of ranked clans to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of ranked clans with clan capital rankings
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if location does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<RankedClan> getLocationCapitalClanRankings(int locationId, Integer limit, String after, String before) {
        return getRankedClans("/locations/" + locationId + "/rankings/capitals", limit, after, before);
    }

    /**
     * Retrieves player rankings for a specific location (main village).
     *
     * Returns players ranked by their main village trophy count within
     * the specified geographical location. Supports pagination.
     *
     * @param locationId location identifier for rankings
     * @param limit maximum number of ranked players to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of ranked players with position and trophy information
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if location does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<RankedPlayer> getLocationPlayerRankings(int locationId, Integer limit, String after, String before) {
        return getRankedPlayers("/locations/" + locationId + "/rankings/players", limit, after, before);
    }

    /**
     * Retrieves builder base player rankings for a specific location.
     *
     * Returns players ranked by their builder base trophy count within
     * the specified geographical location. Supports pagination.
     *
     * @param locationId location identifier for rankings
     * @param limit maximum number of ranked players to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of ranked players with builder base rankings
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if location does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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
            if (items.isArray()) for (var it : items) result.add(convert(it, RankedClan.class));
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

    /**
     * Retrieves all available leagues for the main village.
     *
     * Returns league information including names and trophy requirements
     * for progression through the competitive ladder system.
     *
     * @param limit maximum number of leagues to return (null for no limit)
     * @return list of available leagues with progression requirements
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<League> searchLeagues(Integer limit) { return getLeagues("/leagues", limit); }

    /**
     * Retrieves information about a specific league by its ID.
     *
     * @param leagueId league identifier
     * @return league details including name and trophy requirements
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if league with the specified ID does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public League getLeague(int leagueId) { return getLeagueByPath("/leagues/" + leagueId); }

    /**
     * Retrieves all available clan capital leagues.
     *
     * Returns league information for the clan capital competitive system
     * including names and trophy requirements for progression.
     *
     * @param limit maximum number of leagues to return (null for no limit)
     * @return list of available capital leagues
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<League> getCapitalLeagues(Integer limit) { return getLeagues("/capitalleagues", limit); }

    /**
     * Retrieves information about a specific clan capital league.
     *
     * @param id capital league identifier
     * @return capital league details including requirements
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if capital league does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public League getCapitalLeague(int id) { return getLeagueByPath("/capitalleagues/" + id); }

    /**
     * Retrieves all available clan war leagues.
     *
     * Returns league information for the clan war league competitive system
     * where clans compete in structured seasonal tournaments.
     *
     * @param limit maximum number of leagues to return (null for no limit)
     * @return list of available war leagues
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<League> getWarLeagues(Integer limit) { return getLeagues("/warleagues", limit); }

    /**
     * Retrieves information about a specific clan war league.
     *
     * @param id war league identifier
     * @return war league details and requirements
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if war league does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public League getWarLeague(int id) { return getLeagueByPath("/warleagues/" + id); }

    /**
     * Retrieves all available builder base leagues.
     *
     * Returns league information for the builder base competitive system
     * including trophy requirements and progression structure.
     *
     * @param limit maximum number of leagues to return (null for no limit)
     * @return list of available builder base leagues
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public java.util.List<League> getBuilderBaseLeagues(Integer limit) { return getLeagues("/builderbaseleagues", limit); }

    /**
     * Retrieves information about a specific builder base league.
     *
     * @param id builder base league identifier
     * @return builder base league details and requirements
     * @throws IllegalStateException if client is not logged in
     * @throws NotFoundException if builder base league does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
    public League getBuilderBaseLeague(int id) { return getLeagueByPath("/builderbaseleagues/" + id); }

    /**
     * Retrieves available seasons for a specific league.
     *
     * Returns season identifiers that can be used to query historical
     * ranking data for the specified league. Supports pagination.
     *
     * @param leagueId league identifier to get seasons for
     * @param limit maximum number of seasons to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of season references with identifiers
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves player rankings for a specific league season.
     *
     * Returns historical ranking data showing player positions and trophy
     * counts for the specified league and season combination.
     *
     * @param leagueId league identifier
     * @param seasonId season identifier (obtained from getLeagueSeasons)
     * @param limit maximum number of ranked players to return
     * @param after pagination cursor for results after this position
     * @param before pagination cursor for results before this position
     * @return list of ranked players for the specified season
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves information about the current Gold Pass season.
     *
     * Returns details about the active Gold Pass including start/end dates,
     * available rewards, and seasonal challenges.
     *
     * @return current Gold Pass season information
     * @throws IllegalStateException if client is not logged in
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves the war log for a specified clan.
     *
     * Returns historical war data including results, opponents, and statistics.
     * The clan's war log must be public for this endpoint to succeed.
     *
     * @param clanTag clan tag to get war log for
     * @param limit maximum number of war log entries to return
     * @return list of war log entries with results and opponent information
     * @throws IllegalStateException if client is not logged in
     * @throws PrivateWarLogException if the clan's war log is private
     * @throws NotFoundException if clan with the specified tag does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves information about a clan's current war.
     *
     * Returns detailed information about the ongoing war including state,
     * team sizes, attack counts, and participating members.
     *
     * @param clanTag clan tag to get current war for
     * @return current war information with both clan and opponent details
     * @throws IllegalStateException if client is not logged in
     * @throws PrivateWarLogException if the clan's war information is private
     * @throws NotFoundException if clan does not exist or has no current war
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves clan war league group information for a clan.
     *
     * Returns the current CWL group including participating clans, round structure,
     * and war tags for accessing individual league wars.
     *
     * @param clanTag clan tag to get CWL group information for
     * @return CWL group with participating clans and round information
     * @throws IllegalStateException if client is not logged in
     * @throws PrivateWarLogException if the clan's war league data is private
     * @throws NotFoundException if clan does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Retrieves detailed information about a specific clan war league war.
     *
     * Uses a war tag obtained from the CWL group to fetch detailed war data
     * including attack results, member participation, and final scores.
     *
     * @param warTag war tag identifier from CWL group rounds
     * @return detailed CWL war information with attack data
     * @throws IllegalStateException if client is not logged in
     * @throws PrivateWarLogException if the war data is private or forbidden
     * @throws NotFoundException if war with the specified tag does not exist
     * @throws RuntimeException if API request fails or response parsing fails
     */
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

    /**
     * Returns the first available API token for diagnostic purposes.
     *
     * This method is primarily intended for debugging and monitoring.
     * Returns null if the client is not logged in or no tokens are available.
     *
     * @return first API token or null if not logged in
     */
    public String getToken() { return tokenRotator != null ? tokenRotator.getAll().get(0) : null; }
}
