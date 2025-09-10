package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import com.clanboards.wars.ClanWar;
import com.clanboards.wars.ClanWarLeagueGroup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CwlTest {
    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void getClanWarLeagueGroup_parsesAndFiltersRounds() {
        String json = "{\n" +
                "  \"state\": \"inWar\",\n" +
                "  \"season\": \"2025-09\",\n" +
                "  \"rounds\": [\n" +
                "    {\"warTags\": [\"#0\", \"#0\"]},\n" +
                "    {\"warTags\": [\"#WAR1\", \"#WAR2\"]}\n" +
                "  ],\n" +
                "  \"clans\": [ {\"tag\": \"#AAA\", \"name\": \"Us\"} ]\n" +
                "}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        ClanWarLeagueGroup group = client.getClanWarLeagueGroup("#2PP");
        assertEquals("inWar", group.getState());
        assertEquals("2025-09", group.getSeason());
        assertEquals(2, group.getNumberOfRounds());
        assertEquals(1, group.getRounds().size()); // filtered out the #0 round
        assertEquals(List.of("#WAR1", "#WAR2"), group.getRounds().get(0));
        assertEquals("#AAA", group.getClans().get(0).getTag());
    }

    @Test
    void getCwlWar_fetchesWar() {
        String json = "{\n  \"state\": \"inWar\", \n  \"teamSize\": 15, \n  \"clan\": {\"tag\": \"#AAA\", \"name\": \"Us\", \"stars\": 10, \"destructionPercentage\": 27.0}, \n  \"opponent\": {\"tag\": \"#BBB\", \"name\": \"Them\", \"stars\": 8, \"destructionPercentage\": 20.5}\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        ClanWar war = client.getCwlWar("#WAR1");
        assertEquals("inWar", war.getState());
        assertEquals(15, war.getTeamSize());
    }
}

