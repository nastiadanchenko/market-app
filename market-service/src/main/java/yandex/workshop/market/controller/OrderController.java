package yandex.workshop.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import yandex.workshop.market.domain.PaymentRequest;
import yandex.workshop.market.service.CartService;
import yandex.workshop.market.service.OrderService;
import yandex.workshop.market.service.PaymentService;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentService paymentService;

    /**
     * Получение страницы со списком заказов
     */
    @GetMapping("/orders")
    public Mono<Rendering> getOrdersPage() {
        return orderService.findAllOrdersForCurrentUser()
            .collectList()
            .map(orders ->
                Rendering.view("orders")
                    .modelAttribute("orders", orders)
                    .build()
            );
    }

    /**
     * Получение страницы с информацией о заказе
     */
    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderPage(@PathVariable("id") Long orderId,
                                        @RequestParam(required = false, defaultValue = "false") Boolean newOrder) {

        return orderService.findOrderById(orderId)
            .map(order ->
                Rendering.view("order")
                    .modelAttribute("order", order)
                    .modelAttribute("newOrder", newOrder)
                    .build()
            );
    }

    /**
     * Оформление заказа из корзины текущего пользователя
     */
    @PostMapping("/buy")
    public Mono<Rendering> booking() {
        return cartService.getCurrentUserCart()
            .flatMap(cart -> {
                if (cart.getTotalPrice() == null || cart.getTotalPrice().signum() <= 0) {
                    return Mono.just(
                        Rendering.redirectTo("/cart/items")
                            .modelAttribute("paymentMessage", "Корзина пуста")
                            .build()
                    );
                }
                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.setAmount(cart.getTotalPrice().doubleValue());

                return paymentService.pay(paymentRequest)
                    .flatMap(resp -> {
                        HttpStatusCode status = resp.getStatusCode();

                        if (status.is2xxSuccessful()) {
                            return orderService.createOrder(cart)
                                .map(order ->
                                    Rendering.redirectTo("/orders/{id}")
                                        .modelAttribute("id", order.id())
                                        .modelAttribute("newOrder", true)
                                        .build()
                                );
                        }

                        if (status.value() == 402) {
                            return Mono.just(
                                Rendering.redirectTo("/cart/items")
                                    .modelAttribute("paymentMessage", "Недостаточно средств")
                                    .build()
                            );
                        }

                        return Mono.just(
                            Rendering.redirectTo("/cart/items")
                                .modelAttribute("paymentMessage", "Платежная услуга недоступна")
                                .build()
                        );
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        HttpStatusCode st = ex.getStatusCode();

                        if (st.value() == 402) {
                            return Mono.just(
                                Rendering.redirectTo("/cart/items")
                                    .modelAttribute("paymentMessage", "Недостаточно средств")
                                    .build()
                            );
                        }

                        return Mono.just(
                            Rendering.redirectTo("/cart/items")
                                .modelAttribute("paymentMessage", "Платежная услуга недоступна")
                                .build()
                        );
                    });

            });
    }

}
