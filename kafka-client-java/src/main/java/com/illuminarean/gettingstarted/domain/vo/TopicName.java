package com.illuminarean.gettingstarted.domain.vo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TopicName {
    public static final String BOOK = "book";
    public static final String BOOK_DLQ = "book.dlq";
    public static final String ORDER = "order";
    public static final String ORDER_DLQ = "order.dlq";
}
