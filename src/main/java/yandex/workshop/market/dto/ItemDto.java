package yandex.workshop.market.dto;

import java.math.BigDecimal;

public record ItemDto(
    Long id,
    String title,
    BigDecimal price,
    Integer count,
    String imgPath,
    String description) {
}
