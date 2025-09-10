package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoldPassTest {
    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void currentGoldPassSeason_parsesTimes() {
        String json = "{\n  \"startTime\": \"2025-09-01T00:00:00.000Z\",\n  \"endTime\": \"2025-09-30T23:59:59.000Z\"\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        GoldPassSeason season = client.getCurrentGoldPassSeason();
        assertTrue(season.getStartTime().startsWith("2025-09-01"));
        assertTrue(season.getEndTime().startsWith("2025-09-30"));
    }
}

