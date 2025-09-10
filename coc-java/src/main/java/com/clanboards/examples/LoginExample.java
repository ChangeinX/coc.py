package com.clanboards.examples;

import com.clanboards.CocClient;
import com.clanboards.Clan;
import com.clanboards.auth.DevSiteAuthenticator;
import com.clanboards.http.DefaultHttpTransport;

public class LoginExample {
    public static void main(String[] args) {
        String email = System.getenv("COC_DEV_EMAIL");
        String password = System.getenv("COC_DEV_PASSWORD");
        if (email == null || password == null) {
            System.err.println("Please set COC_DEV_EMAIL and COC_DEV_PASSWORD environment variables.");
            System.exit(1);
        }

        var transport = new DefaultHttpTransport();
        var authenticator = new DevSiteAuthenticator(transport);
        var client = new CocClient(transport, authenticator);

        client.login(email, password);
        String token = client.getToken();
        System.out.println("Login OK. Token prefix: " + (token != null && token.length() > 8 ? token.substring(0, 8) : "<none>"));
        String tag = args.length > 0 ? args[0] : "#2PP"; // example tag
        try {
            Clan clan = client.getClan(tag);
            System.out.printf("Clan: %s (%s), level=%d, members=%d%n",
                    clan.getName(), clan.getTag(), clan.getLevel(), clan.getMemberCount());
        } catch (RuntimeException ex) {
            System.out.println("API call made, but getClan failed for tag '" + tag + "': " + ex.getMessage());
        }
    }
}
