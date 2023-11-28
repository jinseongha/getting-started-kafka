package com.illuminarean.gettingstarted.config;

import com.illuminarean.gettingstarted.domain.vo.TopicConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component
public class TopicConfig {
    @Value("${app.kafka.broker.partition-count}")
    private int partitionCount;

    @Value("${app.kafka.broker.replica-count}")
    private int replicaCount;

    @Bean
    public KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(TopicConstant.BOOK)
                        .partitions(partitionCount)
                        .replicas(replicaCount)
                        .compact()
                        .build(),
                TopicBuilder.name(TopicConstant.BOOK_DLQ)
                        .partitions(partitionCount)
                        .replicas(replicaCount)
                        .compact()
                        .build());
    }
}
