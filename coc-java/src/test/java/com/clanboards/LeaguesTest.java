package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeaguesTest {
    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void searchAndGetLeagues() {
        String listJson = "{\n  \"items\": [ {\"id\": 29000022, \"name\": \"Legend League\"}, {\"id\": 29000021, \"name\": \"Titan League I\"} ]\n}";
        String oneJson = "{\n  \"id\": 29000022, \"name\": \"Legend League\"\n}";
        HttpTransport fake = req -> {
            String url = req.getUrl();
            if (url.contains("/leagues?" ) || url.endsWith("/leagues")) return new HttpResponse(200, listJson.getBytes(StandardCharsets.UTF_8));
            return new HttpResponse(200, oneJson.getBytes(StandardCharsets.UTF_8));
        };
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<League> leagues = client.searchLeagues(10);
        assertTrue(leagues.size() >= 2);
        League legend = client.getLeague(29000022);
        assertEquals("Legend League", legend.getName());
    }

    @Test
    void capitalAndWarAndBuilderBaseLeagues() {
        String listJson = "{\n  \"items\": [ {\"id\": 85000000, \"name\": \"Capital League I\"} ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, listJson.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        assertEquals(1, client.getCapitalLeagues(1).size());
        assertEquals(1, client.getWarLeagues(1).size());
        assertEquals(1, client.getBuilderBaseLeagues(1).size());
    }

    @Test
    void leagueSeasons_and_SeasonInfo_asRankedPlayers() {
        String seasonsJson = "{\n  \"items\": [ {\"id\": \"2025-08\"}, {\"id\": \"2025-07\"} ]\n}";
        String seasonInfoJson = "{\n  \"items\": [ {\"tag\":\"#P1\",\"name\":\"P1\",\"expLevel\":200,\"trophies\":6500,\"rank\":1,\"previousRank\":1} ]\n}";
        HttpTransport fake = req -> {
            String url = req.getUrl();
            if (url.contains("/seasons?" ) || url.endsWith("/seasons")) return new HttpResponse(200, seasonsJson.getBytes(StandardCharsets.UTF_8));
            return new HttpResponse(200, seasonInfoJson.getBytes(StandardCharsets.UTF_8));
        };
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<SeasonRef> seasons = client.getLeagueSeasons(29000022, 2, null, null);
        assertEquals(2, seasons.size());
        List<RankedPlayer> sInfo = client.getLeagueSeasonInfo(29000022, "2025-08", 1, null, null);
        assertEquals(1, sInfo.size());
        assertEquals(6500, sInfo.get(0).getTrophies());
    }
}

