package com.clanboards.auth;

import com.clanboards.http.HttpRequest;
import com.clanboards.http.HttpResponse;
import com.clanboards.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DevSiteAuthenticatorTest {

    private static String jwtWithIp(String ipCidr) {
        String header = Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = "{\"limits\":[null,{\"cidrs\":[\"" + ipCidr + "\"]}]}";
        String payload = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    @Test
    void obtainToken_usesExistingKeyWithMatchingNameAndIp() {
        String ip = "1.2.3.4";
        String jwt = jwtWithIp(ip + "/32");

        HttpTransport fake = new HttpTransport() {
            @Override
            public HttpResponse execute(HttpRequest request) {
                String url = request.getUrl();
                if (url.endsWith("/api/login")) {
                    String body = "{\n  \"temporaryAPIToken\": \"" + jwt + "\"\n}";
                    return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
                }
                if (url.endsWith("/api/apikey/list")) {
                    String body = "{\n  \"keys\": [\n    {\n      \"id\": \"1\",\n      \"name\": \"Created with coc-java Client\",\n      \"cidrRanges\": [\"" + ip + "/32\"],\n      \"key\": \"existing-token\"\n    }\n  ]\n}";
                    return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
                }
                return new HttpResponse(404, "{}".getBytes(StandardCharsets.UTF_8));
            }
        };

        DevSiteAuthenticator auth = new DevSiteAuthenticator(fake);
        String token = auth.obtainToken("e", "p");
        assertEquals("existing-token", token);
    }

    @Test
    void obtainToken_createsKeyIfNoneMatches() {
        String ip = "5.6.7.8";
        String jwt = jwtWithIp(ip + "/32");
        AtomicReference<HttpRequest> lastReq = new AtomicReference<>();
        HttpTransport fake = request -> {
            lastReq.set(request);
            if (request.getUrl().endsWith("/api/login")) {
                String body = "{\"temporaryAPIToken\":\"" + jwt + "\"}";
                return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
            }
            if (request.getUrl().endsWith("/api/apikey/list")) {
                // empty list -> no matching keys
                return new HttpResponse(200, "{\"keys\":[]}".getBytes(StandardCharsets.UTF_8));
            }
            if (request.getUrl().endsWith("/api/apikey/create")) {
                String body = "{\"key\":{\"key\":\"new-token\"}}";
                return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8));
            }
            if (request.getUrl().endsWith("/api/apikey/revoke")) {
                return new HttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8));
            }
            return new HttpResponse(404, "{}".getBytes(StandardCharsets.UTF_8));
        };

        DevSiteAuthenticator auth = new DevSiteAuthenticator(fake);
        String token = auth.obtainToken("e", "p");
        assertEquals("new-token", token);
    }
}

