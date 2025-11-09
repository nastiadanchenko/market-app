package yandex.workshop.market.dto;

public record PagingDto(
    int pageSize,
    int pageNumber,
    boolean hasPrevious,
    boolean hasNext) {



}

