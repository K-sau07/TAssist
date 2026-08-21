package com.tassist.infrastructure.ai.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds tassist.ai.anthropic.* (Claude Haiku chat, §11.6). */
@ConfigurationProperties(prefix = "tassist.ai.anthropic")
public class AnthropicProperties {
    private String apiKey;
    private String model = "claude-haiku-4-5";
    private String baseUrl = "https://api.anthropic.com/v1";
    private String version = "2023-06-01";
    private int maxTokens = 1024;

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { this.apiKey = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { this.baseUrl = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int v) { this.maxTokens = v; }
}
