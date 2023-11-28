package com.illuminarean.gettingstarted.consumer;

import com.illuminarean.gettingstarted.domain.avro.Book;
import com.illuminarean.gettingstarted.domain.vo.BookConstant;
import com.illuminarean.gettingstarted.domain.vo.TopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class BookConsumer {
    private final TaskExecutor executor = new SimpleAsyncTaskExecutor();

    @KafkaListener(id = "${spring.kafka.consumer.group-id}", topics = TopicConstant.BOOK)
    public void listen(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_KEY) long key,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partitionId,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       @Payload GenericRecord message) {
        log.debug("Received: %s [topic: %s, key: %d, partitionId: %d, offset: %d]"
                .formatted(message, topic, key, partitionId, offset));
        var bookId = Long.parseLong(message.get(BookConstant.ID).toString());
        var title = message.get(BookConstant.TITLE).toString();
        var isbn = message.get(BookConstant.ISBN).toString();
        var authors = List.of(message.get(BookConstant.AUTHORS).toString().split(","));
        var publisher = message.get(BookConstant.PUBLISHER).toString();
        var book = new Book(bookId, title, isbn, authors, publisher);
        this.executor.execute(() -> log.info("Received: " + book));
    }

    @KafkaListener(id = "${app.kafka.consumer.dlq-group-id}", topics = TopicConstant.BOOK_DLQ)
    public void listenDlq(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_KEY) long key,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partitionId,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          @Payload GenericRecord message) {
        log.debug("Received: %s [topic: %s, key: %d, partitionId: %d, offset: %d]"
                .formatted(message, topic, key, partitionId, offset));
        this.executor.execute(() -> log.info("Received from DLQ: " + message.toString()));
    }
}
