package yandex.workshop.market;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.ItemRepository;
import yandex.workshop.market.repository.OrderRepository;

@Import(TestcontainersConfiguration.class)
@DataJpaTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DatabaseTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
        List<Item> items = List.of(
            new Item(null, "Бутылка", "Пластиковая", "bottle.jpg", new BigDecimal("100.00"), 2),
            new Item(null, "Кружка", "Керамическая", "cup.jpg", new BigDecimal("50.00"), 0),
            new Item(null, "Тарелка", "Большая", "plate.jpg", new BigDecimal("75.00"), 3));

        itemRepository.saveAll(items);


        orderRepository.deleteAll();

        Order order1 = new Order();
        OrderItem orderItem1 = new OrderItem(null, items.get(0), order1, 2);
        OrderItem orderItem2 = new OrderItem(null, items.get(2), order1, 1);
        order1.setItems(List.of(orderItem1, orderItem2));
        order1.setTotalSum(275L);

        Order order2 = new Order();
        OrderItem orderItem3 = new OrderItem(null, items.get(1), order2, 3);
        order2.setItems(List.of(orderItem3));
        order2.setTotalSum(150L);

        orderRepository.saveAll(List.of(order1, order2));
    }

    @Nested
    @Description("Тестирует методы ItemRepository")
    class ItemRepositoryTests {
        @Test
        void shouldFindAllItems() {
            List<Item> items = itemRepository.findAll();
            assertThat(items).hasSize(3);
        }

        @Test
        void shouldFindItemsByCountGreaterThanZero() {
            List<Item> items = itemRepository.findByCountGreaterThan(0);
            assertThat(items).hasSize(2);
            assertThat(items).extracting("title").containsExactlyInAnyOrder("Бутылка", "Тарелка");
        }

        @Test
        void shouldResetItemCount() {
            Item item = itemRepository.findAll().get(0);

            entityManager.clear(); //очищаем persistence context

            itemRepository.resetItemCount(item.getId());
            Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getCount()).isZero();
        }

        @Test
        void shouldIncreaseItemCount() {
            Item item = itemRepository.findAll().get(0);
            itemRepository.increaseItemCount(item.getId());

            entityManager.clear(); //очищаем persistence context

            Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getCount()).isEqualTo(item.getCount() + 1);
        }

        @Test
        void shouldReduceItemCount() {
            Item item = itemRepository.findAll().get(0);
            itemRepository.reduceItemCount(item.getId());

            entityManager.clear(); //очищаем persistence context

            Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getCount()).isEqualTo(item.getCount() - 1);
        }

    }

    @Nested
    @Description("Тестирует методы OrderRepository")
    class OrderRepositoryTests {

        @Test
        void shouldFindOrderById() {
            Order existingOrder = orderRepository.findAll().get(0);
            Long orderId = existingOrder.getId();

            Order foundOrder = orderRepository.findById(orderId).orElseThrow();

            assertThat(foundOrder).isNotNull();
            assertThat(foundOrder.getId()).isEqualTo(orderId);
        }

        @Test
        void shouldFindAllOrders() {
            List<Order> orders = orderRepository.findAll();
            assertThat(orders).hasSize(2);

        }

    }


}
