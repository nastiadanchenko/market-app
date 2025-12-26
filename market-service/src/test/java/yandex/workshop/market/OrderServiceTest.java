package yandex.workshop.market;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.entity.Cart;
import yandex.workshop.market.entity.CartItem;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.entity.Users;
import yandex.workshop.market.repository.OrderItemRepository;
import yandex.workshop.market.repository.OrderRepository;
import yandex.workshop.market.service.CartService;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;
import yandex.workshop.market.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceTest {

    @MockitoBean
    private TransactionalOperator transactionalOperator;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ItemService itemService;

    @Autowired
    private OrderService orderService;

    private Users user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        when(transactionalOperator.transactional(any(Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        user = new Users();
        user.setId(10L);

        cart = new Cart();
        cart.setId(1L);
        cart.setUserId(10L);
        cart.setTotalPrice(BigDecimal.valueOf(350));
    }

    @Test
    @DisplayName("findAllOrdersForCurrentUser — возвращает заказы текущего пользователя")
    void findAllOrdersForCurrentUser() {
        when(userService.currentUser()).thenReturn(Mono.just(user));

        Order order1 = new Order(1L, BigDecimal.valueOf(100),10L );
        Order order2 = new Order(2L, BigDecimal.valueOf(200),10L );

        when(orderRepository.findAllByUserId(10L))
            .thenReturn(Flux.just(order1, order2));

        when(orderItemRepository.findByOrderId(anyLong()))
            .thenReturn(Flux.empty());

        StepVerifier.create(orderService.findAllOrdersForCurrentUser())
            .assertNext(dto -> assertThat(dto.id()).isEqualTo(1L))
            .assertNext(dto -> assertThat(dto.id()).isEqualTo(2L))
            .verifyComplete();

        verify(orderRepository).findAllByUserId(10L);
    }

    @Nested
    @DisplayName("Тестирование методов поиска")
    class FindOrderByIdTests {

        @Test
        @DisplayName("findOrderById — заказ найден")
        void testFindOrderByIdSuccess() {
            Order order = new Order(5L, BigDecimal.valueOf(500),10L );

            when(orderRepository.findById(5L)).thenReturn(Mono.just(order));
            when(orderItemRepository.findByOrderId(5L)).thenReturn(Flux.empty());

            StepVerifier.create(orderService.findOrderById(5L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(5L);
                    assertThat(dto.totalSum()).isEqualTo(500L);
                    assertThat(dto.items()).isEmpty();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("findOrderById — заказ не найден, должно выбросится исключение NoSuchElementException")
        void testFindOrderByIdNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Mono.empty());

            StepVerifier.create(orderService.findOrderById(99L))
                .expectErrorSatisfies(ex ->
                    assertThat(ex)
                        .isInstanceOf(NoSuchElementException.class)
                        .hasMessageContaining("99")
                )
                .verify();
        }
    }

    @Test
    @DisplayName("createOrder — создаёт заказ, очищает корзину")
    void testCreateOrder() {
        CartItem ci1 = new CartItem(1L, 1L, 1L, 2, Instant.now());
        CartItem ci2 = new CartItem(2L, 1L, 2L, 3, Instant.now());

        when(cartService.getCartItems(cart.getId()))
            .thenReturn(Flux.just(ci1, ci2));

        when(cartService.clearCart(cart)).thenReturn(Mono.empty());

        when(orderRepository.save(any(Order.class)))
            .thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(5L);
                return Mono.just(o);
            });

        when(orderItemRepository.saveAll(anyList()))
            .thenReturn(Flux.empty());

        when(orderItemRepository.findByOrderId(5L))
            .thenReturn(Flux.just(
                new OrderItem(null, 1L, 5L, 2),
                new OrderItem(null, 2L, 5L, 3)
            ));

        when(itemService.findItemById(1L))
            .thenReturn(Mono.just(
                new ItemDto(1L, "Товар 1", BigDecimal.valueOf(100), 0, "", "")
            ));

        when(itemService.findItemById(2L))
            .thenReturn(Mono.just(
                new ItemDto(2L, "Товар 2", BigDecimal.valueOf(50), 0, "", "")
            ));

        StepVerifier.create(orderService.createOrder(cart))
            .assertNext(dto -> {
                assertThat(dto.id()).isEqualTo(5L);
                assertThat(dto.totalSum()).isEqualTo(350L);
                assertThat(dto.items()).hasSize(2);
            })
            .verifyComplete();

        verify(cartService).clearCart(cart);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder — ошибка если корзина пуста")
    void testCreateOrderOrderItemsStructure() {
        when(cartService.getCartItems(cart.getId()))
            .thenReturn(Flux.empty());

        StepVerifier.create(orderService.createOrder(cart))
            .expectErrorSatisfies(ex ->
                assertThat(ex)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Корзина пуста")
            )
            .verify();
    }
}
