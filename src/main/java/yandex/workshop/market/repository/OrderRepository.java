package yandex.workshop.market.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yandex.workshop.market.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
