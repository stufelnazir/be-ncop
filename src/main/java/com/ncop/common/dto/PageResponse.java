package com.ncop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast(),
                springPage.hasNext(),
                springPage.hasPrevious()
        );
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        boolean first = page <= 0;
        boolean last = page >= totalPages - 1;
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                first,
                last,
                hasNext,
                hasPrevious
        );
    }
}
