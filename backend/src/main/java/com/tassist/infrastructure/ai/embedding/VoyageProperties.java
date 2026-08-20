package com.tassist.infrastructure.ai.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds tassist.embedding.voyage.* (D5/D16: voyage-3.5, 1024-dim). */
@ConfigurationProperties(prefix = "tassist.embedding.voyage")
public class VoyageProperties {
    private String apiKey;
    private String model = "voyage-3.5";
    private int dimension = 1024;
    private String baseUrl = "https://api.voyageai.com/v1";

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { this.apiKey = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public int getDimension() { return dimension; }
    public void setDimension(int v) { this.dimension = v; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { this.baseUrl = v; }
}
