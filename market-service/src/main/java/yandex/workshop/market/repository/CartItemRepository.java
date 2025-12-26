package yandex.workshop.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.CartItem;

@Repository
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Mono<CartItem> findByCartIdAndItemId(Long id, Long itemId);

    Mono<Void> deleteAllByCartId(Long id);

    Mono<Void> deleteByCartIdAndItemId(Long id, Long itemId);

    Flux<CartItem> findByCartId(Long id);
}
