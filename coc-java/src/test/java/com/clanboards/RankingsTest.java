package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingsTest {
    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    @Test
    void clanRankings_parsePointsAndRanks() {
        String json = "{\n  \"items\": [\n    {\"tag\":\"#AAA\",\"name\":\"Alpha\",\"clanLevel\":10,\"members\":50,\"clanPoints\":50000,\"rank\":1,\"previousRank\":2},\n    {\"tag\":\"#BBB\",\"name\":\"Beta\",\"clanLevel\":12,\"members\":48,\"clanPoints\":48000,\"rank\":2,\"previousRank\":1}\n  ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<RankedClan> clans = client.getLocationClanRankings(32000006, 2, null, null);
        assertEquals(2, clans.size());
        assertEquals("Alpha", clans.get(0).getName());
        assertEquals(50000, clans.get(0).getPoints());
        assertEquals(1, clans.get(0).getRank());
    }

    @Test
    void playerRankings_parseTrophies() {
        String json = "{\n  \"items\": [\n    {\"tag\":\"#P1\",\"name\":\"P1\",\"expLevel\":200,\"trophies\":6500,\"rank\":1,\"previousRank\":1},\n    {\"tag\":\"#P2\",\"name\":\"P2\",\"expLevel\":190,\"trophies\":6400,\"rank\":2,\"previousRank\":3}\n  ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<RankedPlayer> players = client.getLocationPlayerRankings(32000006, 2, null, null);
        assertEquals(2, players.size());
        assertEquals(6500, players.get(0).getTrophies());
        assertEquals(1, players.get(0).getRank());
    }

    @Test
    void builderBaseRankings_parseFields() {
        String clansJson = "{\n  \"items\": [\n    {\"tag\":\"#AAA\",\"name\":\"Alpha\",\"clanLevel\":10,\"members\":50,\"clanBuilderBasePoints\":30000,\"rank\":1,\"previousRank\":2}\n  ]\n}";
        String playersJson = "{\n  \"items\": [\n    {\"tag\":\"#P1\",\"name\":\"P1\",\"expLevel\":150,\"builderBaseTrophies\":5000,\"rank\":1,\"previousRank\":2}\n  ]\n}";
        HttpTransport fakeClans = req -> new HttpResponse(200, clansJson.getBytes(StandardCharsets.UTF_8));
        HttpTransport fakePlayers = req -> new HttpResponse(200, playersJson.getBytes(StandardCharsets.UTF_8));
        CocClient client1 = new CocClient(fakeClans, singleTokenAuth("t"));
        client1.login("e","p");
        List<RankedClan> clans = client1.getLocationBuilderBaseClanRankings(32000006, 1, null, null);
        assertEquals(30000, clans.get(0).getBuilderBasePoints());

        CocClient client2 = new CocClient(fakePlayers, singleTokenAuth("t"));
        client2.login("e","p");
        List<RankedPlayer> players = client2.getLocationBuilderBasePlayerRankings(32000006, 1, null, null);
        assertEquals(5000, players.get(0).getBuilderBaseTrophies());
    }

    @Test
    void capitalClanRankings_parseField() {
        String json = "{\n  \"items\": [\n    {\"tag\":\"#AAA\",\"name\":\"Alpha\",\"clanLevel\":10,\"members\":50,\"clanCapitalPoints\":1234,\"rank\":1,\"previousRank\":1}\n  ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("t"));
        client.login("e","p");
        List<RankedClan> clans = client.getLocationCapitalClanRankings(32000006, 1, null, null);
        assertEquals(1234, clans.get(0).getCapitalPoints());
    }
}

