package yandex.workshop.market.entity;

import java.math.BigDecimal;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "items")
public class Item {
    @Id
    private Long id;

    private String title;

    private String description;

    private String imgPath;

    private BigDecimal price = BigDecimal.ZERO;

    private Integer count = 0;

}
