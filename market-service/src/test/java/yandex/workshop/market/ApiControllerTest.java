package yandex.workshop.market;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

import io.r2dbc.spi.ConnectionFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.api.DefaultApi;
import yandex.workshop.market.controller.ApiController;
import yandex.workshop.market.domain.BalanceResponse;
import yandex.workshop.market.domain.PaymentResponse;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.ItemsPageDto;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.PagingDto;
import yandex.workshop.market.dto.mapperDto.ItemMapper;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;
import yandex.workshop.market.service.PaymentService;

@WebFluxTest({ApiController.class, DefaultApi.class})
public class ApiControllerTest {
    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private TransactionalOperator transactionalOperator;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentService paymentService;

    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        itemDto = new ItemDto(
            1L,
            "Title",
            BigDecimal.valueOf(100),
            2,
            "/img.png",
            "Desc"
        );
    }

    @Test
    @DisplayName("GET /items - должен возвращать страницу с товарами")
    void shouldReturnItemPage() throws Exception {
        ItemsPageDto page = new ItemsPageDto(List.of(List.of(ItemMapper.INSTANCE.toEntity(itemDto))),
            new PagingDto(5, 1, false, false));

        Mockito.when(itemService.getItemsPage(any(), any(), anyInt(), anyInt()))
            .thenReturn(Mono.just(page));

        webTestClient.get()
            .uri("/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                String body = r.getResponseBody();
                // Проверяем заголовок HTML (или любой другой маркер)
                assert body.contains("items");
            });
    }

    @Test
    @DisplayName("POST /items - должен добавлять товар в корзину и перенаправлять на /items")
    void shouldAddItemToCart() throws Exception {
        Mockito.when(itemService.actionWithItem(anyLong(), any()))
            .thenReturn(Mono.empty());

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/items")
                .queryParam("id", "1")
                .queryParam("action", "PLUS")
                .build())
            .exchange()
            .expectStatus().is3xxRedirection();

        Mockito.verify(itemService).actionWithItem(1L, Action.PLUS);
    }

    @Test
    @DisplayName("GET /items/{id} - должен возвращать страницу с деталями товара")
    void shouldReturnItemDetails() throws Exception {

        Mockito.when(itemService.findItemById(1L))
            .thenReturn(Mono.just(itemDto));

        webTestClient.get()
            .uri("/items/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                assert r.getResponseBody().contains("Title");
            });
    }

    @Test
    @DisplayName("POST /items/{id} - должен изменять количество товара в корзине и перенаправлять на /items/{id}")
    void shouldChangeItemQuantityInCart() throws Exception {
        Long itemId = itemDto.id();

        Mockito.when(itemService.actionWithItem(1L, Action.MINUS))
            .thenReturn(Mono.just(itemDto));

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/items/{id}")
                .queryParam("action", "MINUS")
                .build(1L))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                    assert r.getResponseBody().contains("Title");
            });

        Mockito.verify(itemService).actionWithItem(1L, Action.MINUS);
    }


    @Test
    @DisplayName("GET /cart/items - должен возвращать страницу с товарами в корзине")
    void shouldReturnCartItemsPage() throws Exception {
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(500.00);

        ResponseEntity<BalanceResponse> responseEntity =
            ResponseEntity.ok(balanceResponse);

        Mockito.when(itemService.findItemsDtoByCountGreaterThanZero())
            .thenReturn(Flux.just(itemDto));
        Mockito.when(itemService.getTotalSum())
            .thenReturn(Mono.just(200L));

        Mockito.when(paymentService.getBalance())
            .thenReturn(Mono.just(500.0));

        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                String body = r.getResponseBody();
                assert body != null;
                assert body.contains("cart");
                assert body.contains("200");
                assert !body.contains("Недостаточно средств");
                assert !body.contains("Платежная услуга недоступна");
            });
    }

    @Test
    @DisplayName("POST /cart/items - длжен изменять количество товара в корзине и перенаправлять на /cart/items")
    void shouldModifyCartItemAndRedirect() throws Exception {
        Mockito.when(itemService.actionWithItem(1L, Action.PLUS))
            .thenReturn(Mono.empty());

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/cart/items")
                .queryParam("id", "1")
                .queryParam("action", "PLUS")
                .build())
            .exchange()
            .expectStatus().is3xxRedirection();

        Mockito.verify(itemService).actionWithItem(1L, Action.PLUS);
    }

    @Test
    @DisplayName("GET /orders -должен возвращать страницу со списком заказов")
    void shouldReturnOrdersPage() throws Exception {
        OrderDto orderDto = new OrderDto(1L, List.of(), 100L);
        Mockito.when(orderService.findAllOrders())
            .thenReturn(Flux.just(orderDto));

        webTestClient.get()
            .uri("/orders")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                assert r.getResponseBody().contains("orders");
            });
    }

    @Test
    @DisplayName("GET /orders/{id} - должен возвращать страницу с информацией о заказе")
    void shouldReturnOrderPage() throws Exception {
        OrderDto orderDto = new OrderDto(1L, List.of(), 100L);
//        when(orderService.findOrderById(1L)).then(orderDto);

        Mockito.when(orderService.findOrderById(1L))
            .thenReturn(Mono.just(orderDto));

        webTestClient.get()
            .uri("/orders/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                assert r.getResponseBody().contains("order");
            });
    }

    @Test
    @DisplayName("POST /buy  - должен создавать заказ и перенаправлять на страницу заказа")
    void shouldCreateOrderAndRedirect() {
        long total = 100L;

        OrderDto orderDto = new OrderDto(1L, List.of(), 100L);

        Mockito.when(orderService.createOrder())
            .thenReturn(Mono.just(orderDto));

        PaymentResponse paymentResponse = new PaymentResponse();
        ResponseEntity<PaymentResponse> paymentEntity =
            ResponseEntity.ok(paymentResponse);

        Mockito.when(itemService.getTotalSum())
            .thenReturn(Mono.just(total));

        Mockito.when(paymentService.pay(any()))
            .thenReturn(Mono.just(ResponseEntity.ok(new PaymentResponse())));

        // when / then
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/orders/1");
    }


}
