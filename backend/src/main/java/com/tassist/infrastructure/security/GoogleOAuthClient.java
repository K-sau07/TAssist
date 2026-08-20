package com.tassist.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Manual Google authorization-code flow (D15). Builds the consent URL, exchanges the
 * code for tokens, and reads sub/email/name from the returned id_token. No Spring oauth2Login.
 */
@Component
public class GoogleOAuthClient {

    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "openid email profile";

    private final GoogleOAuthProperties props;
    private final ObjectMapper json = new ObjectMapper();
    private final RestClient http = RestClient.create();

    public GoogleOAuthClient(GoogleOAuthProperties props) { this.props = props; }

    /** The Google consent-screen URL to redirect the browser to. */
    public String authorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
            .queryParam("client_id", props.getClientId())
            .queryParam("redirect_uri", props.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", SCOPE)
            .queryParam("state", state)
            .queryParam("access_type", "online")
            .queryParam("prompt", "select_account")
            .encode().build().toUriString();
    }

    /** Exchange the authorization code for tokens and extract the Google profile. */
    public GoogleProfile exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("redirect_uri", props.getRedirectUri());
        form.add("grant_type", "authorization_code");

        String body = http.post()
            .uri(TOKEN_ENDPOINT)
            .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(String.class);

        try {
            JsonNode token = json.readTree(body);
            String idToken = token.path("id_token").asText(null);
            if (idToken == null) throw new OAuthException("no id_token in Google response");
            JsonNode claims = decodeJwtPayload(idToken);
            String sub = claims.path("sub").asText(null);
            String email = claims.path("email").asText(null);
            String name = claims.path("name").asText(email);
            if (sub == null || email == null)
                throw new OAuthException("id_token missing sub/email");
            return new GoogleProfile(sub, email, name);
        } catch (Exception e) {
            throw new OAuthException("failed to parse Google token response", e);
        }
    }

    private JsonNode decodeJwtPayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) throw new OAuthException("malformed id_token");
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        return json.readTree(new String(payload, StandardCharsets.UTF_8));
    }

    public boolean isConfigured() { return props.isConfigured(); }

    public record GoogleProfile(String subject, String email, String name) {}
}
