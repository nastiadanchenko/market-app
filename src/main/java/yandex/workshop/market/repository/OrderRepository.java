package yandex.workshop.market.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import yandex.workshop.market.entity.Order;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
