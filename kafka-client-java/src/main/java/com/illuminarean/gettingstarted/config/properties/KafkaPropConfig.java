package com.illuminarean.gettingstarted.config.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("app.kafka")
@Getter
@ToString
@RequiredArgsConstructor
public class KafkaPropConfig {
    @NestedConfigurationProperty
    private final BrokerProp broker;
    @NestedConfigurationProperty
    private final ConsumerProp consumer;
}
