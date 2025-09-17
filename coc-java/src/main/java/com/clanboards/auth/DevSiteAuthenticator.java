package com.clanboards.auth;

import com.clanboards.http.HttpRequest;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Production authenticator that manages API keys through the developer portal.
 *
 * This implementation automatically handles the full authentication workflow:
 * <ul>
 * <li>Logs into developer.clashofclans.com with email/password credentials</li>
 * <li>Determines the client's IP address from temporary tokens</li>
 * <li>Searches for existing compatible API keys</li>
 * <li>Creates new keys when needed, respecting platform limits</li>
 * <li>Manages key lifecycle including cleanup of mismatched keys</li>
 * </ul>
 *
 * The authenticator is designed to be robust and handle common scenarios like
 * IP address changes, key limits, and temporary authentication failures.
 *
 * Thread-safety: This class is thread-safe for concurrent authentication attempts.
 *
 * @see Authenticator
 */
public class DevSiteAuthenticator implements Authenticator {
    public static final String DEFAULT_KEY_NAME = "Created with coc-java Client";
    public static final String DEFAULT_SCOPE = "clash"; // regular access

    private final HttpTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String keyName;
    private final String keyScope;
    private final int keyCount;
    private final String fixedIp; // optional override

    /**
     * Creates an authenticator with default settings.
     *
     * Uses default key name, clash scope, single key, and automatic IP detection.
     *
     * @param transport HTTP transport for making requests to the developer portal
     * @throws NullPointerException if transport is null
     */
    public DevSiteAuthenticator(HttpTransport transport) {
        this(transport, DEFAULT_KEY_NAME, DEFAULT_SCOPE, 1, null);
    }

    /**
     * Creates an authenticator with custom configuration.
     *
     * @param transport HTTP transport for making requests to the developer portal
     * @param keyName name to assign to created API keys (null uses default)
     * @param keyScope scope for API keys ("clash" for standard access)
     * @param keyCount maximum number of keys to manage (clamped to 1-10)
     * @param ip fixed IP address for keys (null for automatic detection)
     * @throws NullPointerException if transport is null
     */
    public DevSiteAuthenticator(HttpTransport transport, String keyName, String keyScope, int keyCount, String ip) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.keyName = keyName != null ? keyName : DEFAULT_KEY_NAME;
        this.keyScope = keyScope != null ? keyScope : DEFAULT_SCOPE;
        this.keyCount = Math.max(1, Math.min(10, keyCount));
        this.fixedIp = ip; // can be null -> derive from temporary token
    }

    /**
     * Obtains API tokens by authenticating with the developer portal.
     *
     * This method performs the complete authentication and key management workflow:
     * 1. Logs into the developer portal with provided credentials
     * 2. Determines the client IP address (fixed or automatic)
     * 3. Searches for existing compatible API keys
     * 4. Creates new keys if needed, cleaning up incompatible ones first
     * 5. Returns the requested number of valid tokens
     *
     * @param email developer account email
     * @param password developer account password
     * @param count number of tokens requested (clamped to 1-10)
     * @return list of valid API tokens
     * @throws IllegalStateException if credentials are invalid
     * @throws RuntimeException if authentication or key management fails
     */
    @Override
    public List<String> obtainTokens(String email, String password, int count) {
        String ip = loginAndDetermineIp(email, password);
        int needed = Math.max(1, Math.min(10, count));
        List<String> tokens = listMatchingKeys(ip);

        if (tokens.size() >= needed) {
            return tokens.subList(0, needed);
        }

        // Not enough tokens. Try deleting mismatched named keys to free space, then create new ones
        maybeRevokeMismatchedKeys(ip);
        while (tokens.size() < needed) {
            String newKey = createKey(ip);
            tokens.add(newKey);
        }
        return tokens.subList(0, needed);
    }

    private String loginAndDetermineIp(String email, String password) {
        Map<String, Object> body = Map.of("email", email, "password", password);
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.POST)
                .url("https://developer.clashofclans.com/api/login")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .body(writeJson(body))
                .build();

        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() == 403) {
            throw new IllegalStateException("Invalid credentials for developer site");
        }
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            throw new RuntimeException("Login failed: HTTP " + resp.getStatusCode());
        }
        try {
            JsonNode root = mapper.readTree(resp.getBody());
            if (fixedIp != null && !fixedIp.isBlank()) return fixedIp;
            String jwt = root.path("temporaryAPIToken").asText(null);
            if (jwt == null) throw new RuntimeException("temporaryAPIToken missing in login response");
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) throw new RuntimeException("Invalid temporaryAPIToken format");
            String payloadB64 = parts[1];
            // pad and decode base64 (similar to Python adding ====)
            int pad = (4 - (payloadB64.length() % 4)) % 4;
            payloadB64 = payloadB64 + "====".substring(0, pad);
            String payloadJson = new String(Base64.getDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            JsonNode payload = mapper.readTree(payloadJson);
            String cidr = payload.path("limits").path(1).path("cidrs").path(0).asText();
            if (cidr == null || cidr.isBlank()) throw new RuntimeException("Could not extract IP from token");
            String ip = cidr.contains("/") ? cidr.substring(0, cidr.indexOf('/')) : cidr;
            return ip;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse login response", e);
        }
    }

    private List<String> listMatchingKeys(String ip) {
        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.POST)
                .url("https://developer.clashofclans.com/api/apikey/list")
                .header("Accept", "application/json")
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            throw new RuntimeException("List keys failed: HTTP " + resp.getStatusCode());
        }
        try {
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode keys = root.path("keys");
            List<String> tokens = new ArrayList<>();
            if (keys.isArray()) {
                for (JsonNode k : keys) {
                    String name = k.path("name").asText("");
                    if (!keyName.equals(name)) continue;
                    boolean hasIp = false;
                    for (JsonNode c : k.path("cidrRanges")) {
                        String cidr = c.asText("");
                        if (cidr.startsWith(ip)) { hasIp = true; break; }
                    }
                    if (!hasIp) continue;
                    String token = k.path("key").asText(null);
                    if (token != null) tokens.add(token);
                    if (tokens.size() >= keyCount) break;
                }
            }
            return tokens;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse key list", e);
        }
    }

    private void maybeRevokeMismatchedKeys(String ip) {
        // Re-list keys and revoke named ones that don't match our IP to free slots
        HttpRequest listReq = HttpRequest.newBuilder()
                .method(HttpRequest.Method.POST)
                .url("https://developer.clashofclans.com/api/apikey/list")
                .header("Accept", "application/json")
                .build();
        HttpResponse listResp = transport.execute(listReq);
        if (listResp.getStatusCode() < 200 || listResp.getStatusCode() >= 300) return;
        try {
            JsonNode root = mapper.readTree(listResp.getBody());
            JsonNode keys = root.path("keys");
            if (!keys.isArray()) return;
            for (JsonNode k : keys) {
                String name = k.path("name").asText("");
                if (!keyName.equals(name)) continue;
                boolean matchesIp = false;
                for (JsonNode c : k.path("cidrRanges")) {
                    String cidr = c.asText("");
                    if (cidr.startsWith(ip)) { matchesIp = true; break; }
                }
                if (!matchesIp) {
                    String id = k.path("id").asText(null);
                    if (id != null) {
                        Map<String, Object> body = Map.of("id", id);
                        HttpRequest revokeReq = HttpRequest.newBuilder()
                                .method(HttpRequest.Method.POST)
                                .url("https://developer.clashofclans.com/api/apikey/revoke")
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .body(writeJson(body))
                                .build();
                        transport.execute(revokeReq);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore revoke failures; creation may still succeed
        }
    }

    private String createKey(String ip) {
        String desc = "Created on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", keyName);
        body.put("description", desc);
        body.put("cidrRanges", List.of(ip));
        body.put("scopes", List.of(keyScope));

        HttpRequest req = HttpRequest.newBuilder()
                .method(HttpRequest.Method.POST)
                .url("https://developer.clashofclans.com/api/apikey/create")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .body(writeJson(body))
                .build();
        HttpResponse resp = transport.execute(req);
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            String b = new String(resp.getBody(), StandardCharsets.UTF_8);
            throw new RuntimeException("Create key failed: HTTP " + resp.getStatusCode() + ": " + b);
        }
        try {
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode keyNode = root.path("key");
            String token = keyNode.path("key").asText(null);
            if (token == null) {
                // some responses may be flat
                token = root.path("key").asText(null);
            }
            if (token == null) throw new RuntimeException("Key missing in create response");
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create response", e);
        }
    }

    private byte[] writeJson(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
