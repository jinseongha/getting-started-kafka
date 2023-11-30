package com.illuminarean.gettingstarted.error;

import com.illuminarean.gettingstarted.domain.avro.Book;
import com.illuminarean.gettingstarted.domain.vo.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Slf4j
public class GeneralExceptionHandler {
    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<String, Book> template) {
        return new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template, (r, e) -> new TopicPartition(TopicName.BOOK_DLQ, 0)),
                new FixedBackOff(1000L, 2));
    }
}
