package yandex.workshop.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.PaymentService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final ItemService itemService;

    private final PaymentService paymentService;


    /**
     * Получение страницы с товарами в корзине
     * Дополнительно: определяем доступность кнопки оформления заказа (canCheckout)
     * и сообщение о состоянии платежного сервиса (paymentMessage).
     *
     * @return страница корзины
     */
    @GetMapping("/items")
    public Mono<Rendering> getCartItemsPage() {
        return itemService.findItemsDtoByCountGreaterThanZero()
            .collectList()
            .flatMap(items ->
                itemService.getTotalSum()
                    .flatMap(total ->
                        paymentService.getBalance()
                            .map(balance -> {

                                boolean canCheckout =
                                    balance != null && balance >= total.doubleValue();

                                return Rendering.view("cart")
                                    .modelAttribute("items", items)
                                    .modelAttribute("total", total)
                                    .modelAttribute("canCheckout", canCheckout)
                                    .modelAttribute(
                                        "paymentMessage",
                                        canCheckout ? "" : "Недостаточно средств"
                                    )
                                    .build();
                            })
                            .onErrorResume(ex ->
                                Mono.just(
                                    Rendering.view("cart")
                                        .modelAttribute("items", items)
                                        .modelAttribute("total", total)
                                        .modelAttribute("canCheckout", false)
                                        .modelAttribute(
                                            "paymentMessage",
                                            "Платежная услуга недоступна"
                                        )
                                        .build()
                                )
                            )
                    ));
    }

    /**
     * Действие с товаром из корзины
     *
     * @param itemId id товара
     * @param action действие с товаром (PLUS - добавить, MINUS - уменьшить, DELETE - удалить)
     * @return страница корзины
     */
    @PostMapping("/items")
    public Mono<Rendering> modifyCartItem(@RequestParam("id") Long itemId,
                                          @RequestParam Action action) {
        return itemService.actionWithItem(itemId, action)
            .then(Mono.fromCallable(() -> Rendering.redirectTo("/cart/items").build()));
    }

}
