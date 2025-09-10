package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.exceptions.PrivateWarLogException;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import com.clanboards.wars.ClanWar;
import com.clanboards.wars.ClanWarLogEntry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarsTest {
    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void getWarLog_parsesItems() {
        String json = "{\n  \"items\": [ {\n    \"result\": \"win\", \"teamSize\": 15, \"clan\": {\"tag\": \"#AAA\", \"name\": \"Us\", \"stars\": 40, \"destructionPercentage\": 95.5}, \n    \"opponent\": {\"tag\": \"#BBB\", \"name\": \"Them\", \"stars\": 30, \"destructionPercentage\": 80.0}\n  } ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<ClanWarLogEntry> log = client.getWarLog("#2PP", 1);
        assertEquals(1, log.size());
        assertEquals("win", log.get(0).getResult());
        assertEquals("Us", log.get(0).getClan().getName());
    }

    @Test
    void getWarLog_private_throws() {
        String body = "{\"reason\":\"accessDenied\",\"message\":\"Access denied. Private war log\"}";
        HttpTransport fake = req -> new HttpResponse(403, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertThrows(PrivateWarLogException.class, () -> client.getWarLog("#2PP", 1));
    }

    @Test
    void getCurrentWar_parsesWar() {
        String json = "{\n  \"state\": \"inWar\", \n  \"teamSize\": 15, \n  \"clan\": {\"tag\": \"#AAA\", \"name\": \"Us\", \"stars\": 10, \"destructionPercentage\": 27.0}, \n  \"opponent\": {\"tag\": \"#BBB\", \"name\": \"Them\", \"stars\": 8, \"destructionPercentage\": 20.5}\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        ClanWar war = client.getCurrentWar("#2PP");
        assertEquals("inWar", war.getState());
        assertEquals(15, war.getTeamSize());
        assertEquals("Us", war.getClan().getName());
    }
}

