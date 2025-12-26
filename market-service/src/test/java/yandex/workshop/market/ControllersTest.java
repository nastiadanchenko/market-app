package yandex.workshop.market;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.api.DefaultApi;
import yandex.workshop.market.config.SecurityConfig;
import yandex.workshop.market.controller.CartController;
import yandex.workshop.market.controller.ItemController;
import yandex.workshop.market.controller.OrderController;
import yandex.workshop.market.domain.PaymentResponse;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.CartDto;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.ItemsPageDto;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.PagingDto;
import yandex.workshop.market.entity.Cart;
import yandex.workshop.market.service.CartService;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;
import yandex.workshop.market.service.PaymentService;

@WebFluxTest({ItemController.class,
    CartController.class,
    OrderController.class,
    DefaultApi.class
})
@Import(SecurityConfig.class)
public class ControllersTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentService paymentService;

    private WebTestClient authClient;

    private ItemDto itemDto;
    private Cart cart;

    @BeforeEach
    void setUp() {
        authClient = webTestClient.mutateWith(
            SecurityMockServerConfigurers.mockJwt()
                .jwt(jwt -> jwt.subject("kc-user-1"))
        );

        itemDto = new ItemDto(
            1L,
            "Title",
            BigDecimal.valueOf(100),
            2,
            "/img.png",
            "Desc"
        );

        cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setTotalPrice(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("GET /items - должен возвращать страницу с товарами")
    void shouldReturnItemPage() {
        ItemsPageDto page = new ItemsPageDto(
            List.of(List.of(itemDto)),
            new PagingDto(5, 1, false, false)
        );

        Mockito.when(itemService.getItemsPage(any(), any(), anyInt(), anyInt()))
            .thenReturn(Mono.just(page));

        authClient.get()
            .uri("/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> body.contains("items"));
    }

    @Test
    @DisplayName("POST /items - должен добавлять товар в корзину и перенаправлять на /items")
    void shouldAddItemToCart() {
        Mockito.when(cartService.actionWithItem(1L, Action.PLUS))
            .thenReturn(Mono.empty());

        authClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/items")
                .queryParam("id", "1")
                .queryParam("action", "PLUS")
                .build())
            .exchange()
            .expectStatus().is3xxRedirection();

        Mockito.verify(cartService).actionWithItem(1L, Action.PLUS);

    }

    @Test
    @DisplayName("GET /items/{id} - должен возвращать страницу с деталями товара")
    void shouldReturnItemDetails() {
        Mockito.when(itemService.findItemById(1L))
            .thenReturn(Mono.just(itemDto));

        authClient.get()
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
    void shouldChangeItemQuantityInCart() {
        Long itemId = itemDto.id();

        Mockito.when(cartService.actionWithItem(itemId, Action.MINUS))
            .thenReturn(Mono.empty());

        Mockito.when(itemService.findItemById(itemId))
            .thenReturn(Mono.just(itemDto));

        authClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/items/{id}")
                .queryParam("action", "MINUS")
                .build(1L))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(r -> {
                String body = r.getResponseBody();
                assert body != null;
                assert body.contains("Title");
            });

        Mockito.verify(cartService).actionWithItem(itemId, Action.MINUS);
        Mockito.verify(itemService).findItemById(itemId);
    }


    @Test
    @DisplayName("GET /cart/items - должен возвращать страницу с товарами в корзине")
    void shouldReturnCartPage() {
        CartDto cartDto = new CartDto(
            1L,
            List.of(),
            BigDecimal.valueOf(100)
        );

        Mockito.when(cartService.getCurrentUserCartDto())
            .thenReturn(Mono.just(cartDto));

        Mockito.when(paymentService.getBalance())
            .thenReturn(Mono.just(500.0));

        authClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    @DisplayName("POST /cart/items - длжен изменять количество товара в корзине и перенаправлять на /cart/items")
    void shouldModifyCartItemAndRedirect() throws Exception {
        Mockito.when(cartService.actionWithItem(itemDto.id(), Action.PLUS))
            .thenReturn(Mono.empty());

        authClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/cart/items")
                .queryParam("id", "1")
                .queryParam("action", "PLUS")
                .build())
            .exchange()
            .expectStatus().is3xxRedirection();

        Mockito.verify(cartService).actionWithItem(itemDto.id(), Action.PLUS);
    }

    @Test
    @DisplayName("GET /orders -должен возвращать страницу со списком заказов")
    void shouldReturnOrdersPage() throws Exception {
        OrderDto order = new OrderDto(1L, List.of(), BigDecimal.valueOf(100).longValue());
        Mockito.when(orderService.findAllOrdersForCurrentUser())
            .thenReturn(Flux.just(order));

        authClient.get()
            .uri("/orders")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> body.contains("orders"));
    }

    @Test
    @DisplayName("GET /orders/{id} - должен возвращать страницу с информацией о заказе")
    void shouldReturnOrderPage() throws Exception {
        OrderDto order = new OrderDto(1L, List.of(), BigDecimal.valueOf(100).longValue());

        Mockito.when(orderService.findOrderById(1L))
            .thenReturn(Mono.just(order));

        authClient.get()
            .uri("/orders/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> body.contains("order"));
    }

    @Test
    @DisplayName("POST /buy  - должен создавать заказ и перенаправлять на страницу заказа")
    void shouldCreateOrderAndRedirect() {
        OrderDto order = new OrderDto(1L, List.of(), BigDecimal.valueOf(100).longValue());

        Mockito.when(cartService.getCurrentUserCart())
            .thenReturn(Mono.just(cart));

        Mockito.when(paymentService.pay(any()))
            .thenReturn(Mono.just(ResponseEntity.ok(new PaymentResponse())));

        Mockito.when(orderService.createOrder(cart))
            .thenReturn(Mono.just(order));

        authClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/orders/1");
    }
}



