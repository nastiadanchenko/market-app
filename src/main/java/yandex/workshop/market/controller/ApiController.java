package yandex.workshop.market.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.Sorter;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;

@Controller
@RequiredArgsConstructor
public class ApiController {

    private final ItemService itemService;

    private final OrderService orderService;

    /**
     * Получение страницы со списком товаров
     *
     * @param search     поисковый запрос по названию товара
     * @param sort       сортировка (NO - без сортировки, ALPHA - по алфавиту, PRICE - по цене)
     * @param pageNumber номер страницы
     * @param pageSize   количество товаров на странице
     * @return страница со списком товаров
     */

    @GetMapping({"/", "/items"})
    public Mono<Rendering> getItemsPage(@RequestParam(required = false, defaultValue = "") String search,
                                        @RequestParam(required = false, defaultValue = "NO") Sorter sort,
                                        @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                        @RequestParam(required = false, defaultValue = "5") Integer pageSize) {

        return itemService.getItemsPage(search, sort, pageNumber, pageSize)
            .map(itemsPageDto ->
                Rendering.view("items")
                    .modelAttribute("items", itemsPageDto.itemsRows())
                    .modelAttribute("search", search)
                    .modelAttribute("sort", sort)
                    .modelAttribute("paging", itemsPageDto.paging())
                    .build()
            );

    }

    /**
     * Действие с товаром из списка товаров
     *
     * @param itemId     id товара
     * @param search     поисковый запрос по названию товара
     * @param sort       сортировка (NO - без сортировки, ALPHA - по алфавиту, PRICE - по цене)
     * @param pageNumber номер страницы
     * @param pageSize   количество товаров на странице
     * @param action     действие с товаром (PLUS - добавить, MINUS - уменьшить, DELETE - удалить)
     * @return страница со списком товаров
     */
    @PostMapping({"/items"})
    public Mono<Rendering> addToCart(@RequestParam("id") Long itemId,
                                     @RequestParam(required = false, defaultValue = "") String search,
                                     @RequestParam(required = false, defaultValue = "NO") String sort,
                                     @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                     @RequestParam(required = false, defaultValue = "5") Integer pageSize,
                                     @RequestParam Action action) {

        return itemService.actionWithItem(itemId, action)
            .then(Mono.fromCallable(() -> Rendering.redirectTo("/items")
                .modelAttribute("search", search)
                .modelAttribute("sort", sort)
                .modelAttribute("pageNumber", pageNumber)
                .modelAttribute("pageSize", pageSize)
                .build()));
    }

    /**
     * Получение страницы с информацией о товаре
     *
     * @param itemId id товара
     * @return страница товара
     */
    @GetMapping({"/items/{id}"})
    public Mono<Rendering> getItemPage(@PathVariable("id") Long itemId) {

        return itemService.findItemById(itemId)
            .map(item ->
                Rendering.view("item")
                    .modelAttribute("item", item)
                    .build()
            );
    }

    /**
     * Добавление товара в корзину
     *
     * @param itemId id товара
     * @param action действие с товаром (PLUS - добавить, MINUS - уменьшить, DELETE - удалить)
     * @return страница товара
     */
    @PostMapping({"/items/{id}"})
    public Mono<Rendering> addToCart(@PathVariable("id") Long itemId,
                                     @RequestParam Action action) {
        return itemService.actionWithItem(itemId, action)
            .map(item ->
                Rendering.view("item")
                    .modelAttribute("item", item)
                    .build()
            );
    }

    /**
     * Получение страницы с товарами в корзине
     * @return страница корзины
     */
    @GetMapping("/cart/items")
    public Mono<Rendering> getCartItemsPage() {
        return itemService.findItemsDtoByCountGreaterThanZero()
            .collectList()
            .flatMap(items ->
                itemService.getTotalSum()
                    .map(total ->
                        Rendering.view("cart")
                            .modelAttribute("items", items)
                            .modelAttribute("total", total)
                            .build()
                    )
            );
    }


    /**
     * Действие с товаром из корзины
     * @param itemId    id товара
     * @param action    действие с товаром (PLUS - добавить, MINUS - уменьшить, DELETE - удалить)
     * @return страница корзины
     */
    @PostMapping("/cart/items")
    public Mono<Rendering> modifyCartItem(@RequestParam("id") Long itemId,
                                          @RequestParam Action action) {
        return itemService.actionWithItem(itemId, action)
            .then(Mono.fromCallable(() -> Rendering.redirectTo("/cart/items").build()));
    }

    /**
     * Получение страницы со списком заказов
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
     * @param orderId   id заказа
     * @param newOrder  флаг, указывающий, что заказ только что создан
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
     * Создание заказа из товаров в корзине
     * @return перенаправление на страницу созданного заказа
     */
    @PostMapping("/buy")
    public Mono<Rendering> booking() {
        Mono<OrderDto> order = orderService.createOrder();

        return order.map(o ->
            Rendering.redirectTo("/orders/{id}")
                .modelAttribute("id", o.id())
                .modelAttribute("newOrder", true)
                .build()
        );
    }

}
