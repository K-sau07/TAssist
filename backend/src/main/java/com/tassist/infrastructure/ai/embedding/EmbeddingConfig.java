package com.tassist.infrastructure.ai.embedding;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables Voyage embedding config binding. */
@Configuration
@EnableConfigurationProperties(VoyageProperties.class)
public class EmbeddingConfig {
}
