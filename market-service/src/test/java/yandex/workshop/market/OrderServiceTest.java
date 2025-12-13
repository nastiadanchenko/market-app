package yandex.workshop.market;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.OrderItemRepository;
import yandex.workshop.market.repository.OrderRepository;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;

@SpringBootTest
public class OrderServiceTest {

    @MockitoBean
    private TransactionalOperator transactionalOperator;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private ItemService itemService;

    @Autowired
    private OrderService orderService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        item1 = new Item(1L, "Товар 1", "desc", "", new BigDecimal("100.00"), 2);
        item2 = new Item(2L, "Товар 2", "desc", "", new BigDecimal("50.00"), 3);

    }

    @Test
    @DisplayName("findAllOrders — возвращает список всех заказов")
    void testFindAllOrders() {
        Order o1 = new Order(1L,  100L);
        Order o2 = new Order(2L, 200L);

        when(orderRepository.findAll()).thenReturn(Flux.just(o1, o2));
        when(orderItemRepository.findByOrderId(anyLong())).thenReturn(Flux.empty());
        when(itemService.findItemById(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(orderService.findAllOrders())
            .expectNextMatches(dto -> dto.id() == 1L)
            .expectNextMatches(dto -> dto.id() == 2L)
            .verifyComplete();

        verify(orderRepository).findAll();
    }

    @Nested
    @DisplayName("Тестирование методов поиска")
    class FindOrderByIdTests {

        @Test
        @DisplayName("findOrderById — заказ найден")
        void testFindOrderByIdSuccess() {
            Order order = new Order(10L, 500L);

            when(orderRepository.findById(10L)).thenReturn(Mono.just(order));
            when(orderItemRepository.findByOrderId(10L)).thenReturn(Flux.empty());

            StepVerifier.create(orderService.findOrderById(10L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(10L);
                    assertThat(dto.totalSum()).isEqualTo(500L);
                })
                .verifyComplete();

            verify(orderRepository).findById(10L);
        }

        @Test
        @DisplayName("findOrderById — заказ НЕ найден → NoSuchElementException")
        void testFindOrderByIdNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Mono.empty());

            StepVerifier.create(orderService.findOrderById(99L))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(NoSuchElementException.class)
                        .hasMessageContaining("99");
                })
                .verify();

            verify(orderRepository).findById(99L);
        }
    }

    @Test
    @DisplayName("createOrder — создаёт заказ, очищает корзину и возвращает DTO")
    void testCreateOrder() {
        when(itemService.findItemsByCountGreaterThanZero())
            .thenReturn(Flux.just(item1, item2));

        when(itemService.getTotalSum()).thenReturn(Mono.just(350L)); // 100*2 + 50*3 = 350

        Order savedOrder = new Order();
        savedOrder.setId(5L);
        savedOrder.setTotalSum(350L);

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));

        OrderItem oi1 = new OrderItem(null, 1L, 5L, 2);
        OrderItem oi2 = new OrderItem(null, 2L, 5L, 3);

        when(orderItemRepository.findByOrderId(5L)).thenReturn(Flux.just(oi1, oi2));

        when(itemService.findItemById(1L))
            .thenReturn(Mono.just(new ItemDto(
                1L, "Товар 1", BigDecimal.valueOf(100), 2, "", "desc"
            )));

        when(itemService.findItemById(2L))
            .thenReturn(Mono.just(new ItemDto(
                2L, "Товар 2", BigDecimal.valueOf(50), 3, "", "desc"
            )));

        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(itemService.actionWithItem(anyLong(), eq(Action.DELETE))).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrder())
            .assertNext(dto -> {
                assertThat(dto.id()).isEqualTo(5L);
                assertThat(dto.totalSum()).isEqualTo(350L);
                assertThat(dto.items()).hasSize(2);
            })
            .verifyComplete();

        verify(itemService).actionWithItem(1L, Action.DELETE);
        verify(itemService).actionWithItem(2L, Action.DELETE);
        verify(orderRepository).save(any(Order.class));
        verify(itemService).getTotalSum();
    }

    @Test
    @DisplayName("createOrder — корректно создаёт OrderItem для каждого Item")
    void testCreateOrderOrderItemsStructure() {
        when(itemService.findItemsByCountGreaterThanZero())
            .thenReturn(Flux.just(item1));

        when(itemService.getTotalSum()).thenReturn(Mono.just(200L)); // 100 * 2

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            return Mono.just(order);
        });

        when(orderItemRepository.saveAll(anyList()))
            .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));

        OrderItem created = new OrderItem(null, 1L, 1L, 2);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(created));

        when(itemService.findItemById(1L))
            .thenReturn(Mono.just(
                new ItemDto(1L, "Item1", new BigDecimal("100"), 2, null, null)
            ));

        when(itemService.actionWithItem(anyLong(), eq(Action.DELETE))).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrder())
            .assertNext(dto -> {
                assertThat(dto.totalSum()).isEqualTo(200L);
                assertThat(dto.items()).hasSize(1);
                assertThat(dto.items().get(0).id()).isEqualTo(1L);
                assertThat(dto.items().get(0).count()).isEqualTo(2);
            })
            .verifyComplete();

        verify(itemService).actionWithItem(1L, Action.DELETE);
    }
}
