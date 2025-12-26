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
import yandex.workshop.market.entity.Cart;
import yandex.workshop.market.entity.CartItem;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.entity.Role;
import yandex.workshop.market.entity.UserRole;
import yandex.workshop.market.entity.Users;
import yandex.workshop.market.repository.CartItemRepository;
import yandex.workshop.market.repository.CartRepository;
import yandex.workshop.market.repository.ItemRepository;
import yandex.workshop.market.repository.OrderItemRepository;
import yandex.workshop.market.repository.OrderRepository;
import yandex.workshop.market.repository.UserRepository;

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private R2dbcEntityTemplate template;

    private Long userId;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @BeforeAll
    static void init(@Autowired R2dbcEntityTemplate template) {
        template.getDatabaseClient()
            .sql("""
            create table if not exists users (
                id bigserial primary key,
                name varchar(255),
                enabled boolean,
                created_at timestamp,
                updated_at timestamp,
                mail varchar(255),
                keycloak_id uuid unique
            );

            create table if not exists roles (
                id bigserial primary key,
                name varchar(50)
            );

            create table if not exists users_roles (
                id bigserial primary key,
                user_id bigint references users(id),
                role_id bigint references roles(id)
            );

            create table if not exists items (
                id bigserial primary key,
                title varchar(255),
                description varchar(1000),
                img_path varchar(255),
                price numeric not null,
                count int not null
            );

            create table if not exists orders (
                id bigserial primary key,
                user_id bigint references users(id),
                total_sum numeric not null
            );

            create table if not exists orders_items (
                id bigserial primary key,
                item_id bigint references items(id),
                order_id bigint references orders(id),
                count int
            );

            create table if not exists carts (
                id bigserial primary key,
                user_id bigint references users(id),
                created_at timestamp,
                updated_at timestamp,
                total_price numeric
            );

            create table if not exists carts_items (
                id bigserial primary key,
                cart_id bigint references carts(id),
                item_id bigint references items(id),
                count int,
                added_at timestamp
            );
        """)
            .then()
            .block();
    }

    @BeforeEach
    void setUp() {
        template.delete(UserRole.class).all().block();
        template.delete(Role.class).all().block();
        template.delete(CartItem.class).all().block();
        template.delete(Cart.class).all().block();
        template.delete(OrderItem.class).all().block();
        template.delete(Order.class).all().block();
        template.delete(Item.class).all().block();
        template.delete(Users.class).all().block();

        Users user = new Users();
        user.setName("user1");
        user.setEnabled(true);

        userId = userRepository.save(user)
            .map(Users::getId)
            .block();

        List<Item> items = List.of(
            new Item(null, "Бутылка", "Пластик", "b.jpg", new BigDecimal("100"), 2),
            new Item(null, "Кружка", "Керамика", "c.jpg", new BigDecimal("50"), 0),
            new Item(null, "Тарелка", "Большая", "p.jpg", new BigDecimal("75"), 3)
        );

        List<Item> savedItems =
            itemRepository.saveAll(items).collectList().block();

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalSum(new BigDecimal("275"));

        Order savedOrder = orderRepository.save(order).block();

        orderItemRepository.saveAll(List.of(
            new OrderItem(null, savedItems.get(0).getId(), savedOrder.getId(), 2),
            new OrderItem(null, savedItems.get(2).getId(), savedOrder.getId(), 1)
        )).collectList().block();

    }

    @Nested
    @Description("Тестирует методы ItemRepository")
    class ItemRepositoryTests {
        @Test
        void findItemsWithPositiveCount() {
            StepVerifier.create(itemRepository.findItemsByCountGreaterThan(0))
                .expectNextCount(2)
                .verifyComplete();
        }

        @Test
        void increaseAndDecreaseCount() {
            Item item = itemRepository.findAll().next().block();

            StepVerifier.create(itemRepository.increaseItemCount(item.getId()))
                .assertNext(i -> assertThat(i.getCount()).isEqualTo(item.getCount() + 1))
                .verifyComplete();

            StepVerifier.create(itemRepository.reduceItemCount(item.getId()))
                .assertNext(i -> assertThat(i.getCount()).isEqualTo(item.getCount()))
                .verifyComplete();
        }


    }

    @Nested
    @Description("Тестирует методы OrderRepository")
    class OrderRepositoryTests {

        @Test
        void findOrdersByUser() {
            StepVerifier.create(orderRepository.findAllByUserId(userId))
                .expectNextCount(1)
                .verifyComplete();
        }

        @Test
        void findOrderByIdAndUser() {
            Order order = orderRepository.findAll().next().block();

            StepVerifier.create(
                    orderRepository.findByIdAndUserId(order.getId(), userId)
                )
                .assertNext(o -> assertThat(o.getUserId()).isEqualTo(userId))
                .verifyComplete();
        }

    }

    @Nested
    @Description("Тестирует методы CartRepository")
    class CartRepositoryTests {

        @Test
        void createAndFindCartByUser() {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setTotalPrice(BigDecimal.ZERO);

            cartRepository.save(cart).block();

            StepVerifier.create(cartRepository.findByUserId(userId))
                .assertNext(c -> assertThat(c.getUserId()).isEqualTo(userId))
                .verifyComplete();
        }
    }
}
