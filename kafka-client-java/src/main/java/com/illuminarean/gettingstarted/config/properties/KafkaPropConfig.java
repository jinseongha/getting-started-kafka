package com.illuminarean.gettingstarted.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka")
public record KafkaPropConfig(
        BrokerProp broker,
        ConsumerProp consumer
) {
    public record BrokerProp(
            @Positive int partitionCount,
            @Positive int replicationCount
    ) {
    }

    public record ConsumerProp(
            @NotBlank String dlqGroupId
    ) {
    }
}
