package com.illuminarean.gettingstarted.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan("com.illuminarean.gettingstarted.config.properties")
public class PropertyConfig {
}
