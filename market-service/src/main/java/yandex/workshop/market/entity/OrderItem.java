package yandex.workshop.market.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders_items")
public class OrderItem {
    @Id
    private Long id;

    private Long itemId;

    private Long orderId;

    private Integer count;
}
