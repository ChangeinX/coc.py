package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpRequest;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAndMetaTest {

    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void getPlayer_fetchesAndParses() {
        String json = "{\n  \"tag\": \"#ABC\",\n  \"name\": \"PlayerOne\",\n  \"townHallLevel\": 16\n}";
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpTransport fake = req -> { captured.set(req); return new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8)); };
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        Player p = client.getPlayer(" abc ");
        assertEquals("#ABC", p.getTag());
        assertEquals("PlayerOne", p.getName());
        assertEquals(16, p.getTownHallLevel());
        assertTrue(captured.get().getUrl().contains("/players/%23ABC"));
    }

    @Test
    void verifyPlayerToken_postsBody_andParsesOk() {
        AtomicReference<HttpRequest> cap = new AtomicReference<>();
        String body = "{\"status\":\"ok\"}";
        HttpTransport fake = req -> { cap.set(req); return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8)); };
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        boolean ok = client.verifyPlayerToken("#ABC", "secret");
        assertTrue(ok);
        assertEquals("POST", cap.get().getMethod().name());
        assertEquals("application/json", cap.get().getHeaders().get("Content-Type"));
        assertTrue(new String(cap.get().getBody(), StandardCharsets.UTF_8).contains("\"token\":\"secret\""));
    }

    @Test
    void verifyPlayerToken_throwsNotFound_on404() {
        HttpTransport fake = req -> new HttpResponse(404, "{}".getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertThrows(com.clanboards.exceptions.NotFoundException.class,
                () -> client.verifyPlayerToken("#MISSING", "tok"));
    }

    @Test
    void verifyPlayerToken_returnsFalse_onNonOkStatus() {
        String body = "{\"status\":\"invalid\"}";
        HttpTransport fake = req -> new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertFalse(client.verifyPlayerToken("#ABC", "tok"));
    }

    @Test
    void verifyPlayerToken_returnsFalse_onMalformedJson() {
        String body = "{this is not json";
        HttpTransport fake = req -> new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertFalse(client.verifyPlayerToken("#ABC", "tok"));
    }

    @Test
    void verifyPlayerToken_acceptsCaseInsensitiveOk() {
        String body = "{\"status\":\"OK\"}";
        HttpTransport fake = req -> new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertTrue(client.verifyPlayerToken("#ABC", "tok"));
    }

    @Test
    void verifyPlayerToken_throwsRuntime_on5xx() {
        HttpTransport fake = req -> new HttpResponse(500, "error".getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertThrows(RuntimeException.class, () -> client.verifyPlayerToken("#ABC", "tok"));
    }

    @Test
    void getLabels_returnsItems() {
        String json = "{\n  \"items\": [ {\"id\": 1, \"name\": \"Clan Wars\"}, {\"id\": 2, \"name\": \"Clan Games\"} ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<Label> clanLabels = client.getClanLabels();
        List<Label> playerLabels = client.getPlayerLabels();
        assertEquals(2, clanLabels.size());
        assertEquals(2, playerLabels.size());
        assertEquals("Clan Wars", clanLabels.get(0).getName());
    }

    @Test
    void locations_endpoints_work() {
        String listJson = "{\n  \"items\": [ {\"id\": 32000006, \"name\": \"United States\", \"isCountry\": true, \"countryCode\": \"US\"} ]\n}";
        String oneJson = "{\n  \"id\": 32000006, \"name\": \"United States\", \"isCountry\": true, \"countryCode\": \"US\"\n}";
        AtomicReference<HttpRequest> cap = new AtomicReference<>();
        HttpTransport fake = req -> {
            cap.set(req);
            String url = req.getUrl();
            if (url.endsWith("/locations") || url.contains("/locations?"))
                return new HttpResponse(200, listJson.getBytes(StandardCharsets.UTF_8));
            return new HttpResponse(200, oneJson.getBytes(StandardCharsets.UTF_8));
        };
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<Location> locations = client.searchLocations(1);
        assertEquals(1, locations.size());
        Location us = client.getLocation(32000006);
        assertEquals("United States", us.getName());
        assertTrue(cap.get().getUrl().contains("/locations/32000006"));
    }
}
