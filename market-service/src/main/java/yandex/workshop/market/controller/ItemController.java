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
import yandex.workshop.market.dto.Sorter;
import yandex.workshop.market.service.ItemService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;


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
}
