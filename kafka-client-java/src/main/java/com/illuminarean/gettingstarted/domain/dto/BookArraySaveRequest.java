package com.illuminarean.gettingstarted.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookArraySaveRequest {
    private List<BookSaveRequest> books;
}
