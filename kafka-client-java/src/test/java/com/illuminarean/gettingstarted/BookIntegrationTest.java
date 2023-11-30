package com.illuminarean.gettingstarted;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.illuminarean.gettingstarted.domain.avro.Book;
import com.illuminarean.gettingstarted.domain.dto.BookSaveRequest;
import com.illuminarean.gettingstarted.domain.vo.BookInfo;
import com.illuminarean.gettingstarted.domain.vo.TopicName;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.hamcrest.KafkaMatchers;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class BookIntegrationTest extends AbstractKafkaClusterSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.[schema.registry.url]}")
    private String schemaRegistryUrl;

    @Value("${app.kafka.broker.partition-count}")
    private int partitionCount;

    @Value("${app.kafka.broker.replica-count}")
    private int replicaCount;

    private static Book mapToAvroRecord(BookSaveRequest book) {
        return Book.newBuilder()
                .setId(book.getId())
                .setTitle(book.getTitle())
                .setIsbn(book.getIsbn())
                .setAuthors(book.getAuthors())
                .setPublisher(book.getPublisher())
                .build();
    }

    private static void assertHasKeyAndValue(ConsumerRecord<Long, GenericRecord> record, Long key, Book book) {
        assertThat(record, KafkaMatchers.hasKey(key));
        assertEquals(record.value().get(BookInfo.ID), book.getId());
        assertEquals(record.value().get(BookInfo.TITLE), book.getTitle());
        assertEquals(record.value().get(BookInfo.ISBN), book.getIsbn());
        assertEquals(record.value().get(BookInfo.AUTHORS), book.getAuthors());
        assertEquals(record.value().get(BookInfo.PUBLISHER), book.getPublisher());
    }

    private Map<String, Object> getAdminProps() {
        final var props = Maps.<String, Object>newHashMap();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }

    private Map<String, Object> getProducerProps() {
        final var producerProps = KafkaTestUtils.producerProps(bootstrapServers);
        producerProps.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url", schemaRegistryUrl);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        return producerProps;
    }

    private Map<String, Object> getConsumerProps(String group) {
        final var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, group, "true");
        consumerProps.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + "url", schemaRegistryUrl);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        return consumerProps;
    }

    private <T> BlockingQueue<ConsumerRecord<Long, GenericRecord>> getRecordBlockingQueue(String topic, String group, T recordClass) {
        final var consumerProps = getConsumerProps(group);
        final var consumerFactory = new DefaultKafkaConsumerFactory<Long, T>(consumerProps);
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
        try (final var adminClient = AdminClient.create(getAdminProps())) {
            final var topicList = topics.stream()
                    .map(topic -> new NewTopic(topic, partitionCount, (short) replicaCount))
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

    private List<BookSaveRequest> getBooks() {
        return List.of(
                new BookSaveRequest(1L, "Kafka: The Definitive Guide, 2nd Edition", "978-1492043089",
                        List.of("Gwen Shapira", "Todd Palino", "Rajini Sivaram", "Krit Petty"), "O'Reilly Media"),
                new BookSaveRequest(2L, "Kafka in Action", "978-1617295232",
                        List.of("Dylan Scott, Viktor Gamov, Dave Klein"), "Manning Publications"),
                new BookSaveRequest(3L, "Kafka Streams in Action", "978-1617294471",
                        List.of("William P. Bejeck Jr."), "Manning Publications"),
                new BookSaveRequest(4L, "Building Event-Driven Microservices", "978-1492057895",
                        List.of("Adam Bellemare"), "O'Reilly Media"),
                new BookSaveRequest(5L, "Designing Data-Intensive Applications", "978-1449373320",
                        List.of("Martin Kleppmann"), "O'Reilly Media"),
                new BookSaveRequest(6L, "Implementing Domain-Driven Design", "978-0321834577",
                        List.of("Vaughn Vernon"), "Addison-Wesley Professional"),
                new BookSaveRequest(7L, "Fundamentals of Software Architecture", "978-1492043454",
                        List.of("Mark Richards", "Neal Ford"), "O'Reilly Media"),
                new BookSaveRequest(8L, "Clean Code", "978-0132350884",
                        List.of("Robert C. Martin"), "Pearson"),
                new BookSaveRequest(9L, "Clean Architecture", "978-0134494166",
                        List.of("Robert C. Martin"), "Pearson"),
                new BookSaveRequest(10L, "Design Patterns", "978-0201633610",
                        List.of("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"), "Addison-Wesley Professional"));
    }

    @AfterEach
    void tearDown() {
        Awaitility.await().untilAsserted(() -> executeDeleteTopicOperations(List.of(TopicName.BOOK)));
        Awaitility.await().untilAsserted(() -> executeCreateTopicOperations(List.of(TopicName.BOOK)));
    }

    @Test
    void saveBook_successful() throws Exception {
        // arrange
        final var book = BookSaveRequest.builder()
                .id(11L)
                .title("Kafka: The Definitive Guide, 2nd Edition")
                .isbn("978-1492043089")
                .authors(List.of("Gwen Shapira", "Todd Palino", "Rajini Sivaram", "Krit Petty"))
                .publisher("O'Reilly Media")
                .build();

        // act
        final var result = mockMvc.perform(post("/api/v1/produce/books/1")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(book)));

        // assert
        result.andDo(print())
                .andExpect(status().isOk());

        final var records = getRecordBlockingQueue(TopicName.BOOK, "book-test-group", Book.class);
        final var received = records.poll(10, TimeUnit.SECONDS);
        assert received != null;
        assertHasKeyAndValue(received, received.key(), mapToAvroRecord(book));
        assertEquals(0, records.size());
    }


    @Test
    void saveBooks_successful() throws Exception {
        // arrange
        final var books = getBooks();
        final var bookMap = books.stream()
                .collect(Collectors.toMap(BookSaveRequest::getId, BookIntegrationTest::mapToAvroRecord));

        // act
        final var result = mockMvc.perform(post("/api/v1/produce/books")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(books)));

        // assert
        result.andDo(print())
                .andExpect(status().isOk());

        final var records = getRecordBlockingQueue(TopicName.BOOK, "books-test-group", Book.class);
        for (int i = 0; i < books.size(); i++) {
            final var received = records.poll(10, TimeUnit.SECONDS);
            assert received != null;
            assertHasKeyAndValue(received, received.key(), bookMap.get(received.key()));
        }
        assertEquals(0, records.size());
    }
}
