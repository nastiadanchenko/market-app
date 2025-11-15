package yandex.workshop.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "items")
public class Item {
    @Id
    @SequenceGenerator(name = "item_sequence", sequenceName = "item_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_sequence")
    private Long id;

    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private String imgPath;

    @NotNull
    @Column(nullable = false, columnDefinition = "numeric default 0.00")
    private BigDecimal price = BigDecimal.ZERO;

    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer count = 0;

}
