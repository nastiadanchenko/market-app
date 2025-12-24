package yandex.workshop.market.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
    Long id,
    List<CartItemDto> items,
    BigDecimal total
) {
}
