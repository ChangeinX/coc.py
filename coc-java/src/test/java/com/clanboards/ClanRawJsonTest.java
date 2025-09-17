package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClanRawJsonTest {

    private static Authenticator singleTokenAuth(String token) {
        return new Authenticator() {
            @Override
            public List<String> obtainTokens(String email, String password, int count) {
                return List.of(token);
            }
        };
    }

    private static byte[] sampleClan() {
        try {
            return Files.readAllBytes(Path.of("src/test/resources/clans/CLAN.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getClan_rawJsonDisabledByDefault() {
        HttpTransport fake = req -> new HttpResponse(200, sampleClan());
        CocClient client = new CocClient(fake, singleTokenAuth("token"));
        client.login("email", "password");
        Clan clan = client.getClan("#2PP");
        assertNull(clan.getRawJson());
    }

    @Test
    void getClan_rawJsonAvailableWhenEnabled() {
        HttpTransport fake = req -> new HttpResponse(200, sampleClan());
        CocClient client = new CocClient(fake, singleTokenAuth("token"), true);
        client.login("email", "password");
        Clan clan = client.getClan("#2PP");
        assertNotNull(clan.getRawJson());
        assertEquals("#2PP", clan.getRawJson().path("tag").asText());
        assertEquals("Example Clan", clan.getRawJson().path("name").asText());
    }

    @Test
    void searchClans_setsRawJsonOnResultsWhenEnabled() {
        String body = "{\n  \"items\": [\n    {\"tag\":\"#AAA\",\"name\":\"Alpha\",\"clanLevel\":3,\"members\":30},\n    {\"tag\":\"#BBB\",\"name\":\"Beta\",\"clanLevel\":4,\"members\":35}\n  ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("token"), true);
        client.login("email", "password");
        List<Clan> clans = client.searchClans("Alpha", 2);
        assertEquals(2, clans.size());
        assertNotNull(clans.get(0).getRawJson());
        assertEquals("#AAA", clans.get(0).getRawJson().path("tag").asText());
    }

    @Test
    void getMembers_setsRawJsonOnMembersWhenEnabled() {
        String body = "{\n  \"items\": [\n    {\"tag\":\"#P1\",\"name\":\"Player1\",\"role\":\"member\",\"expLevel\":100,\"trophies\":5000,\"builderBaseTrophies\":4000}\n  ]\n}";
        HttpTransport fake = req -> new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
        CocClient client = new CocClient(fake, singleTokenAuth("token"), true);
        client.login("email", "password");
        List<ClanMember> members = client.getMembers("#TAG", null, null, null);
        assertEquals(1, members.size());
        assertNotNull(members.get(0).getRawJson());
        assertEquals("#P1", members.get(0).getRawJson().path("tag").asText());
    }
}
