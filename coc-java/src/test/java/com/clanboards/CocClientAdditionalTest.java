package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpRequest;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CocClientAdditionalTest {

    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void searchClans_requiresAtLeastOneFilter() {
        HttpTransport fake = req -> new HttpResponse(200, "{\"items\":[]}".getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertThrows(IllegalArgumentException.class, () -> client.searchClans(null, null));
    }

    @Test
    void searchClans_returnsClans_andBuildsQuery() {
        AtomicReference<HttpRequest> cap = new AtomicReference<>();
        String body = "{\n  \"items\": [\n    {\"tag\":\"#AAA\",\"name\":\"Alpha\",\"clanLevel\":3,\"members\":30},\n    {\"tag\":\"#BBB\",\"name\":\"Beta\",\"clanLevel\":5,\"members\":45}\n  ]\n}";
        HttpTransport fake = req -> { cap.set(req); return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8)); };
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<Clan> out = client.searchClans("My Clan", 2);
        assertEquals(2, out.size());
        assertEquals("Alpha", out.get(0).getName());
        assertTrue(cap.get().getUrl().contains("/clans?"));
        assertTrue(cap.get().getUrl().contains("name=My+Clan"));
        assertTrue(cap.get().getUrl().contains("limit=2"));
    }

    @Test
    void getMembers_parsesItems() {
        String body = "{\n  \"items\": [\n    {\"tag\":\"#P1\",\"name\":\"Player1\",\"role\":\"member\",\"expLevel\":100,\"trophies\":5000,\"builderBaseTrophies\":4000},\n    {\"tag\":\"#P2\",\"name\":\"Player2\",\"role\":\"elder\",\"expLevel\":120,\"trophies\":5500,\"builderBaseTrophies\":4200}\n  ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<ClanMember> members = client.getMembers("#2PP", 2, null, null);
        assertEquals(2, members.size());
        assertEquals("Player1", members.get(0).getName());
        assertEquals(100, members.get(0).getExpLevel());
    }
}

