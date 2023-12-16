package com.illuminarean.gettingstarted.config.properties;

import jakarta.validation.constraints.NotBlank;

public record ConsumerProp(
        @NotBlank String dlqGroupId
) {
}
