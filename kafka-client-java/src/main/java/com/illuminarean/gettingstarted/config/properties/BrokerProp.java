package com.illuminarean.gettingstarted.config.properties;

import jakarta.validation.constraints.Positive;

public record BrokerProp(
        @Positive int partitionCount,
        @Positive int replicationCount
) {
}
