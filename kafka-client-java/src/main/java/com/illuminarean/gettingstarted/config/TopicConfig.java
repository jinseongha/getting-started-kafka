package com.illuminarean.gettingstarted.config;

import com.illuminarean.gettingstarted.config.properties.KafkaPropConfig;
import com.illuminarean.gettingstarted.domain.vo.TopicName;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@RequiredArgsConstructor
public class TopicConfig {
    private final KafkaPropConfig kafkaPropConfig;

    @Bean
    public KafkaAdmin.NewTopics topics() {
        final var partitionCount = kafkaPropConfig.broker().partitionCount();
        final var replicationCount = kafkaPropConfig.broker().replicationCount();
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(TopicName.BOOK)
                        .partitions(partitionCount)
                        .replicas(replicationCount)
                        .compact()
                        .build(),
                TopicBuilder.name(TopicName.BOOK_DLQ)
                        .partitions(partitionCount)
                        .replicas(replicationCount)
                        .compact()
                        .build());
    }
}
