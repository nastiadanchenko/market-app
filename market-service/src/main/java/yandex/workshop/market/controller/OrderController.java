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
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;
import yandex.workshop.market.service.PaymentService;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final ItemService itemService;

    private final PaymentService paymentService;

    private final OrderService orderService;

    /**
     * Получение страницы со списком заказов
     *
     * @return страница со списком заказов
     */
    @GetMapping("/orders")
    private Mono<Rendering> getOrdersPage() {
        return orderService.findAllOrders()
            .collectList()
            .map(orders ->
                Rendering.view("orders")
                    .modelAttribute("orders", orders)
                    .build()
            );
    }

    /**
     * Получение страницы с информацией о заказе
     *
     * @param orderId  id заказа
     * @param newOrder флаг, указывающий, что заказ только что создан
     * @return страница заказа
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
     * Создание заказа из товаров в корзине.
     * Перед созданием пытаемся провести платёж через external payments service.
     * При успешном платеже — создаём заказ и редиректим на страницу заказа.
     * При 402 (insufficient funds) или 503 (service unavailable) — редиректим назад в корзину с сообщением.
     *
     * @return перенаправление на страницу созданного заказа
     */
    @PostMapping("/buy")
    public Mono<Rendering> booking() {
        return itemService.getTotalSum()
            .flatMap(total -> {
                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.setAmount(total.doubleValue());

                return paymentService.pay(paymentRequest)
                    .flatMap(resp -> {
                        HttpStatusCode status = resp.getStatusCode();

                        if (status.is2xxSuccessful()) {
                            return orderService.createOrder()
                                .map(o ->
                                    Rendering.redirectTo("/orders/{id}")
                                        .modelAttribute("id", o.id())
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
