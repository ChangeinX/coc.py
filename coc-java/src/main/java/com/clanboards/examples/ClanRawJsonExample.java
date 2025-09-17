package com.clanboards.examples;

import com.clanboards.CocClient;
import com.clanboards.Clan;
import com.clanboards.auth.Authenticator;
import com.clanboards.auth.DevSiteAuthenticator;
import com.clanboards.http.DefaultHttpTransport;
import com.clanboards.http.HttpTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fetches a clan using the Java client with raw JSON enabled and prints the payload to stdout.
 * The clan tag is provided as the first argument. Credentials must be supplied via COC_EMAIL and
 * COC_PASSWORD environment variables so both Python and Java clients share the same login.
 */
public final class ClanRawJsonExample {
    private ClanRawJsonExample() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: ClanRawJsonExample <clanTag>");
            System.exit(1);
        }
        String tag = args[0];

        String email = System.getenv("COC_EMAIL");
        if (email == null || email.isBlank()) {
            System.err.println("COC_EMAIL environment variable is required");
            System.exit(2);
        }
        String password = System.getenv("COC_PASSWORD");
        if (password == null || password.isBlank()) {
            System.err.println("COC_PASSWORD environment variable is required");
            System.exit(3);
        }

        HttpTransport transport = new DefaultHttpTransport();
        Authenticator authenticator = new DevSiteAuthenticator(transport);
        CocClient client = new CocClient(transport, authenticator, true);

        try {
            client.login(email, password, 1, 10);
            Clan clan = client.getClan(tag);
            JsonNode raw = clan.getRawJson();
            if (raw == null) {
                raw = new ObjectMapper().valueToTree(clan);
            }
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writeValueAsString(raw));
        } finally {
            // DefaultHttpTransport does not require explicit shutdown, but leave room for hooks later.
        }
    }
}
