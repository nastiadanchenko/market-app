package yandex.workshop.market;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.mapperDto.OrderDtoMapper;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.OrderRepository;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;

@SpringBootTest
public class OrderServiceTest {

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private ItemService itemService;

    @Autowired
    private OrderService orderService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = new Item(1L, "Товар 1", "desc", "", new BigDecimal("100.00"), 2);
        item2 = new Item(2L, "Товар 2", "desc", "", new BigDecimal("50.00"), 3);
    }

    @Test
    @DisplayName("findAllOrders — возвращает список всех заказов")
    void testFindAllOrders() {
        Order o1 = new Order(1L, List.of(), 100L);
        Order o2 = new Order(2L, List.of(), 200L);

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        List<OrderDto> result = orderService.findAllOrders();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);

        verify(orderRepository).findAll();
    }

    @Nested
    @DisplayName("Тестирование методов поиска")
    class FindOrderByIdTests {

        @Test
        @DisplayName("findOrderById — заказ найден")
        void testFindOrderByIdSuccess() {
            Order order = new Order(10L, List.of(), 500L);

            when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

            OrderDto dto = orderService.findOrderById(10L);

            assertThat(dto.id()).isEqualTo(10L);
            assertThat(dto.totalSum()).isEqualTo(500L);

            verify(orderRepository).findById(10L);
        }

        @Test
        @DisplayName("findOrderById — заказ НЕ найден → NoSuchElementException")
        void testFindOrderByIdNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findOrderById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");

            verify(orderRepository).findById(99L);
        }
    }

    @Test
    @DisplayName("createOrder — создаёт заказ, очищает корзину и возвращает DTO")
    void testCreateOrder() {
        when(itemService.findItemsByCountGreaterThanZero())
            .thenReturn(List.of(item1, item2));

        when(itemService.getTotalSum()).thenReturn(350L); // 100*2 + 50*3 = 350

        Order saved = new Order();
        saved.setId(5L);
        saved.setTotalSum(350L);

        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        OrderDto result = orderService.createOrder();

        verify(itemService).actionWithItem(1L, Action.DELETE);
        verify(itemService).actionWithItem(2L, Action.DELETE);

        verify(orderRepository).save(any(Order.class));
        verify(itemService).getTotalSum();

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.totalSum()).isEqualTo(350L);
    }

    @Test
    @DisplayName("createOrder — корректно создаёт OrderItem для каждого Item")
    void testCreateOrderOrderItemsStructure() {
        when(itemService.findItemsByCountGreaterThanZero())
            .thenReturn(List.of(item1));

        when(itemService.getTotalSum()).thenReturn(200L); // 100 * 2

          when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderDto dto = orderService.createOrder();

        Order orderCaptured =
            OrderDtoMapper.INSTANCE.toEntity(dto);

        assertThat(dto.totalSum()).isEqualTo(200L);
        assertThat(orderCaptured.getItems()).hasSize(1);

        OrderItem orderItem = orderCaptured.getItems().get(0);

        assertThat(orderItem.getItem().getId()).isEqualTo(1L);
        assertThat(orderItem.getCount()).isEqualTo(2);

        verify(itemService).actionWithItem(1L, Action.DELETE);
    }
}
