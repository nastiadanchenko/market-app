package yandex.workshop.market.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.Item;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long>{

    @Query(value = "update items set count = count + 1 WHERE id = :id returning *")
    Mono<Item> increaseItemCount(Long id);

    @Query(value = "update items set count = count - 1 WHERE id = :id AND count > 0 returning *")
    Mono<Item> reduceItemCount(Long id);

    Flux<Item> findItemsByCountGreaterThan(int i);

    @Query("update items set count = 0 WHERE id = :itemId returning *")
    Mono<Item> resetItemCount(Long itemId);
}
