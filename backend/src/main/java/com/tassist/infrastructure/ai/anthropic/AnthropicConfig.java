package com.tassist.infrastructure.ai.anthropic;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables Anthropic chat config binding. */
@Configuration
@EnableConfigurationProperties(AnthropicProperties.class)
public class AnthropicConfig {
}
