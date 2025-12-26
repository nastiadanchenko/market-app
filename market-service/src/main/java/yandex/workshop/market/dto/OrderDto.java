package yandex.workshop.market.dto;

import java.util.List;

public record OrderDto(
    Long id,
    List<ItemDto> items,
    Long totalSum
) {
}
