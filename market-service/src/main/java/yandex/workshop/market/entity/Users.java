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
@Table("users")
public class Users {
    @Id
    private Long id;

    private String name;

    private String password;

    private Boolean enabled;
    @Column("created_at")
    private Instant created;
    @Column("updated_at")
    private Instant updated;
}
