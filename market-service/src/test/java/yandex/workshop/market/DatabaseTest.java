package yandex.workshop.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.ItemRepository;
import yandex.workshop.market.repository.OrderItemRepository;
import yandex.workshop.market.repository.OrderRepository;
import java.util.ArrayList;

@DataR2dbcTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@Import({TestCacheConfig.class})
public class DatabaseTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Autowired
    R2dbcEntityTemplate template;

    @BeforeAll
    static void init(@Autowired R2dbcEntityTemplate template) {
        template.getDatabaseClient()
            .sql("CREATE SEQUENCE IF NOT EXISTS orders_id_seq AS BIGINT; " +
                "CREATE TABLE if not exists public.orders " +
                "(id        bigint NOT NULL primary key default (nextval('orders_id_seq'::regclass)), " +
                "total_sum int8   not null default 0)")
            .then().block();
        template.getDatabaseClient()
            .sql("CREATE SEQUENCE IF NOT EXISTS items_id_seq AS BIGINT; " +
                "CREATE TABLE if not exists public.items " +
                "(id          bigint NOT NULL primary key default (nextval('items_id_seq'::regclass))," +
                "    title       varchar(255)      NULL," +
                "    description varchar(1000)     NULL," +
                "    img_path    varchar(255)      NOT NULL," +
                "    price       numeric DEFAULT 0.00 NOT NULL," +
                "    count       int4    DEFAULT 0 NOT NULL)")
            .then()
            .block();

        template.getDatabaseClient()
            .sql("CREATE SEQUENCE IF NOT EXISTS orders_items_id_seq AS BIGINT; " +
                "CREATE TABLE if not exists public.orders_items\n" +
                "(id       bigint NOT NULL primary key default (nextval('orders_items_id_seq'::regclass))," +
                "    item_id  int8      NOT NULL," +
                "    order_id int8      NOT NULL," +
                "    count    int4      NULL," +
                "    CONSTRAINT fk_orders_items_item FOREIGN KEY (item_id) REFERENCES public.items (id) ON DELETE CASCADE," +
                "    CONSTRAINT fk_orders_items_order FOREIGN KEY (order_id) REFERENCES public.orders (id) ON DELETE CASCADE)")
            .then()
            .block();
    }

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        orderItemRepository.deleteAll().block();
        List<Item> items = List.of(
            new Item(null, "Бутылка", "Пластиковая", "bottle.jpg", new BigDecimal("100.00"), 2),
            new Item(null, "Кружка", "Керамическая", "cup.jpg", new BigDecimal("50.00"), 0),
            new Item(null, "Тарелка", "Большая", "plate.jpg", new BigDecimal("75.00"), 3));

        List<Item> savedItems = itemRepository.saveAll(items).collectList().block();

        Order order1 = new Order();
        order1.setTotalSum(275L);

        Order savedOrder1 = orderRepository.save(order1).block();

        OrderItem oi1 = new OrderItem(null, savedItems.get(0).getId(), savedOrder1.getId(), 2);
        OrderItem oi2 = new OrderItem(null, savedItems.get(2).getId(), savedOrder1.getId(), 1);

        orderItemRepository.saveAll(List.of(oi1, oi2)).collectList().block();


        Order order2 = new Order();
        order2.setTotalSum(150L);
        Order savedOrder2 = orderRepository.save(order2).block();

        OrderItem oi3 = new OrderItem(null, savedItems.get(1).getId(), savedOrder2.getId(), 3);
        orderItemRepository.save(oi3).block();

        orderItemRepository.saveAll(List.of(oi1, oi2)).collectList().block();
    }

    @Nested
    @Description("Тестирует методы ItemRepository")
    class ItemRepositoryTests {
        @Test
        void shouldFindAllItems() {
            StepVerifier.create(itemRepository.findAll())
                .expectNextCount(3)
                .verifyComplete();
        }

        @Test
        void shouldFindItemsByCountGreaterThanZero() {
            StepVerifier.create(itemRepository.findItemsByCountGreaterThan(0))
                .recordWith(ArrayList::new)
                .expectNextCount(2)
                .consumeRecordedWith(list -> {
                    List<String> titles = list.stream().map(Item::getTitle).toList();
                    assertThat(titles).containsExactlyInAnyOrder("Бутылка", "Тарелка");
                })
                .verifyComplete();
        }

        @Test
        void shouldResetItemCount() {
            Long id = itemRepository.findAll()
                .next().map(Item::getId)
                .block();

            StepVerifier.create(itemRepository.resetItemCount(id))
                .assertNext(i -> assertThat(i.getCount()).isZero())
                .verifyComplete();
        }

        @Test
        void shouldIncreaseItemCount() {
            Item first = itemRepository.findAll().next().block();

            StepVerifier.create(itemRepository.increaseItemCount(first.getId()))
                .assertNext(updated -> assertThat(updated.getCount()).isEqualTo(first.getCount() + 1))
                .verifyComplete();
        }

        @Test
        void shouldReduceItemCount() {
            Item first = itemRepository.findAll().next().block();

            StepVerifier.create(itemRepository.reduceItemCount(first.getId()))
                .assertNext(updated -> assertThat(updated.getCount()).isEqualTo(first.getCount() - 1))
                .verifyComplete();
        }

    }

    @Nested
    @Description("Тестирует методы OrderRepository")
    class OrderRepositoryTests {

        @Test
        void shouldFindOrderById() {
            Long id = orderRepository.findAll().next().map(Order::getId).block();

            StepVerifier.create(orderRepository.findById(id))
                .assertNext(o -> assertThat(o.getId()).isEqualTo(id))
                .verifyComplete();
        }

        @Test
        void shouldFindAllOrders() {
            StepVerifier.create(orderRepository.findAll())
                .expectNextCount(2)
                .verifyComplete();

        }

    }


}
