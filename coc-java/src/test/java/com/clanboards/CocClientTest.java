package com.clanboards;

import com.clanboards.auth.Authenticator;
import com.clanboards.exceptions.NotFoundException;
import com.clanboards.http.HttpRequest;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import com.clanboards.util.TagUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CocClientTest {

    private static byte[] readResource(String path) {
        try {
            return Files.readAllBytes(Path.of("coc-java/src/test/resources/" + path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void login_setsBearerToken_and_getClan_usesAuthHeader() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpTransport fakeTransport = req -> {
            captured.set(req);
            String json = "{\n  \"tag\": \"#123ABC0\",\n  \"name\": \"My Clan\",\n  \"clanLevel\": 10,\n  \"members\": 50\n}";
            return new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        };
        Authenticator fakeAuth = new Authenticator() {
            @Override
            public java.util.List<String> obtainTokens(String email, String password, int count) {
                return java.util.List.of("test-token");
            }
        };

        CocClient client = new CocClient(fakeTransport, fakeAuth);
        client.login("email@example.com", "password");
        assertEquals("test-token", client.getToken());

        Clan clan = client.getClan(" 123abc o");
        assertNotNull(clan);
        assertEquals("#123ABC0", clan.getTag());
        assertEquals("My Clan", clan.getName());
        assertEquals(10, clan.getLevel());
        assertEquals(50, clan.getMemberCount());

        HttpRequest req = captured.get();
        assertNotNull(req);
        assertEquals("Bearer test-token", req.getHeaders().get("Authorization"));
        assertTrue(req.getUrl().endsWith("/clans/%23123ABC0"), "URL should encode # as %23");
    }

    @Test
    void getClan_404_throwsNotFound() {
        HttpTransport fakeTransport = req -> new HttpResponse(404, "{}".getBytes(StandardCharsets.UTF_8));
        Authenticator fakeAuth = new Authenticator() {
            @Override
            public java.util.List<String> obtainTokens(String email, String password, int count) {
                return java.util.List.of("token");
            }
        };
        CocClient client = new CocClient(fakeTransport, fakeAuth);
        client.login("e", "p");
        assertThrows(NotFoundException.class, () -> client.getClan("#ABC"));
    }

    @Test
    void correctTag_behavesLikePython() {
        assertEquals("#123ABC0", TagUtil.correctTag(" 123aBc O"));
    }

    @Test
    void tokenRotation_roundRobinAcrossRequests() {
        java.util.List<String> seenAuths = new java.util.ArrayList<>();
        HttpTransport fakeTransport = req -> {
            seenAuths.add(req.getHeaders().get("Authorization"));
            String json = "{\"tag\":\"#X\",\"name\":\"C\",\"clanLevel\":1,\"members\":1}";
            return new HttpResponse(200, json.getBytes(StandardCharsets.UTF_8));
        };
        CocClient client = new CocClient(fakeTransport, new Authenticator() {
            @Override
            public java.util.List<String> obtainTokens(String email, String password, int count) {
                return java.util.List.of("unused");
            }
        });
        client.loginWithTokens(java.util.List.of("t1", "t2"), 1000);
        client.getClan("#x");
        client.getClan("#x");
        client.getClan("#x");
        assertEquals(java.util.List.of("Bearer t1", "Bearer t2", "Bearer t1"), seenAuths);
    }
}
