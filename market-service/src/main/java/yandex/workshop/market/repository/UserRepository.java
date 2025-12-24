package yandex.workshop.market.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.Users;

@Repository
public interface UserRepository extends ReactiveCrudRepository<Users, Long> {

    Mono<Users> findByName(String name);

    @Query("select * from users where keycloak_id = :s")
    Mono<Users> findByKeycloakId(String s);
}
