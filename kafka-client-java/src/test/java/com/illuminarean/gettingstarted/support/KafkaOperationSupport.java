package com.illuminarean.gettingstarted.support;

import com.google.common.collect.Maps;
import com.illuminarean.gettingstarted.config.properties.KafkaPropConfig;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@RequiredArgsConstructor
public class KafkaOperationSupport {
    private final KafkaProperties kafkaProperties;
    private final KafkaPropConfig kafkaPropConfig;

    private Map<String, Object> getAdminProps() {
        final var bootstrapServers = kafkaProperties.getBootstrapServers();
        final var props = Maps.<String, Object>newHashMap();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }

    private Map<String, Object> getProducerProps() {
        final var bootstrapServers = String.join(",", kafkaProperties.getBootstrapServers());
        final var schemaRegistryUrl = kafkaProperties.getProperties()
                .get(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url");
        final var producerProps = KafkaTestUtils.producerProps(bootstrapServers);
        producerProps.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url", schemaRegistryUrl);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        return producerProps;
    }

    private Map<String, Object> getConsumerProps(String group) {
        final var bootstrapServers = String.join(",", kafkaProperties.getBootstrapServers());
        final var schemaRegistryUrl = kafkaProperties.getProperties()
                .get(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url");
        final var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, group, "true");
        consumerProps.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url", schemaRegistryUrl);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        return consumerProps;
    }

    public BlockingQueue<ConsumerRecord<Long, GenericRecord>> getRecordBlockingQueue(String topic, String group) {
        final var partitionCount = kafkaPropConfig.getBroker().partitionCount();
        final var consumerProps = getConsumerProps(group);
        final var consumerFactory = new DefaultKafkaConsumerFactory<Long, GenericRecord>(consumerProps);
        final var containerProperties = new ContainerProperties(topic);
        final var container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        final BlockingQueue<ConsumerRecord<Long, GenericRecord>> records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<Long, GenericRecord>) records::add);
        container.setBeanName("recordBlockingQueue");
        container.start();
        ContainerTestUtils.waitForAssignment(container, partitionCount);
        return records;
    }

    public void executeCreateTopicOperations(List<String> topics) {
        final var partitionCount = kafkaPropConfig.getBroker().partitionCount();
        final var replicationCount = kafkaPropConfig.getBroker().replicationCount();
        try (final var adminClient = AdminClient.create(getAdminProps())) {
            final var topicList = topics.stream()
                    .map(topic -> new NewTopic(topic, partitionCount, (short) replicationCount))
                    .toList();
            adminClient.createTopics(topicList);
        }
    }

    public void executeDeleteTopicOperations(List<String> topics) {
        try (final var adminClient = AdminClient.create(getAdminProps())) {
            adminClient.listTopics().names().get().stream()
                    .filter(topics::contains)
                    .forEach(topic -> adminClient.deleteTopics(List.of(topic)));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
