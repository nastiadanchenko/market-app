package yandex.workshop.market.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("carts_items")
public class CartItem {
    @Id
    private Long id;
    @Column("cart_id")
    private Long cartId;
    @Column("item_id")
    private Long itemId;
    @Column("count")
    private Integer count;
    @Column("added_at")
    private Instant addedAt;
}
