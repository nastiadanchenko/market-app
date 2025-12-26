package yandex.workshop.market.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table("roles")
public class Role {
    @Id
    private Long id;

    private String name;
}
