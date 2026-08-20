package com.tassist.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds tassist.google.* (client id/secret + redirect). Empty when OAuth not configured. */
@ConfigurationProperties(prefix = "tassist.google")
public class GoogleOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String v) { this.clientSecret = v; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String v) { this.redirectUri = v; }
}
