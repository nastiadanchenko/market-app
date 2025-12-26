package yandex.workshop.market.dto;

public record OrderItemDto(
    Long id,
    ItemDto item,
    Integer count) {
}
