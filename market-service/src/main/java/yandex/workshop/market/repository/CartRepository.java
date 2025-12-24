package yandex.workshop.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.Cart;
@Repository
public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {
    Mono<Cart>findByUserId(Long userId);
}
