package yandex.workshop.market.dto;

import java.math.BigDecimal;

public record CartItemDto(
    ItemDto item,
    Integer count,
    BigDecimal sum
) {
}
