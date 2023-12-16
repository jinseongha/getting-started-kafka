package com.illuminarean.gettingstarted.consumer;

import com.illuminarean.gettingstarted.domain.avro.Book;
import com.illuminarean.gettingstarted.domain.vo.BookInfo;
import com.illuminarean.gettingstarted.domain.vo.TopicName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookConsumer {
    private final TaskExecutor executor;

    @KafkaListener(id = "${spring.kafka.consumer.group-id}", topics = TopicName.BOOK)
    public void listen(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_KEY) long key,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partitionId,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       @Payload GenericRecord message) {
        log.debug("Received: %s [topic: %s, key: %d, partitionId: %d, offset: %d]"
                .formatted(message, topic, key, partitionId, offset));
        var bookId = Long.parseLong(message.get(BookInfo.ID).toString());
        var title = message.get(BookInfo.TITLE).toString();
        var isbn = message.get(BookInfo.ISBN).toString();
        var authors = List.of(message.get(BookInfo.AUTHORS).toString().split(","));
        var publisher = message.get(BookInfo.PUBLISHER).toString();
        var book = new Book(bookId, title, isbn, authors, publisher);
        executor.execute(() -> log.info("Received: " + book));
    }

    @KafkaListener(id = "${app.kafka.consumer.dlq-group-id}", topics = TopicName.BOOK_DLQ)
    public void listenDlq(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_KEY) long key,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partitionId,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          @Payload GenericRecord message) {
        log.debug("Received: %s [topic: %s, key: %d, partitionId: %d, offset: %d]"
                .formatted(message, topic, key, partitionId, offset));
        executor.execute(() -> log.info("Received from DLQ: " + message.toString()));
    }
}
