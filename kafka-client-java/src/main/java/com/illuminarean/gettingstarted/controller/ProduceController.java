package com.illuminarean.gettingstarted.controller;

import com.illuminarean.gettingstarted.domain.avro.Book;
import com.illuminarean.gettingstarted.domain.dto.BookSaveRequest;
import com.illuminarean.gettingstarted.domain.vo.TopicName;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/produce")
@RequiredArgsConstructor
public class ProduceController {
    private final KafkaTemplate<Long, Book> kafkaTemplate;

    @PostMapping(path = "/books/{bookId}")
    public void saveBook(@PathVariable String bookId, @RequestBody BookSaveRequest book) {
        final var bookRecord = Book.newBuilder()
                .setId(book.getId())
                .setTitle(book.getTitle())
                .setIsbn(book.getIsbn())
                .setAuthors(book.getAuthors())
                .setPublisher(book.getPublisher())
                .build();
        kafkaTemplate.send(TopicName.BOOK, Long.valueOf(bookId), bookRecord);
    }

    @PostMapping(path = "/books")
    public void saveBooks(@RequestBody List<BookSaveRequest> books) {
        books.forEach(book -> {
            final var bookRecord = Book.newBuilder()
                    .setId(book.getId())
                    .setTitle(book.getTitle())
                    .setIsbn(book.getIsbn())
                    .setAuthors(book.getAuthors())
                    .setPublisher(book.getPublisher())
                    .build();
            kafkaTemplate.send(TopicName.BOOK, book.getId(), bookRecord);
        });
    }
}
