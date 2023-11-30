package com.illuminarean.gettingstarted.config;

import com.illuminarean.gettingstarted.domain.vo.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class TopicConfig {
    @Value("${app.kafka.broker.partition-count}")
    private int partitionCount;

    @Value("${app.kafka.broker.replica-count}")
    private int replicaCount;

    @Bean
    public KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(TopicName.BOOK)
                        .partitions(partitionCount)
                        .replicas(replicaCount)
                        .compact()
                        .build(),
                TopicBuilder.name(TopicName.BOOK_DLQ)
                        .partitions(partitionCount)
                        .replicas(replicaCount)
                        .compact()
                        .build());
    }
}
