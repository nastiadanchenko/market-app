package yandex.workshop.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import yandex.workshop.market.entity.Role;

@Repository
public interface RoleRepository extends ReactiveCrudRepository<Role, Long> {
}
