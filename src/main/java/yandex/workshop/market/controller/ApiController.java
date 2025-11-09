package yandex.workshop.market.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.ItemsPage;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.Sorter;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;

@Controller
@RequiredArgsConstructor
public class ApiController {

    private final ItemService itemService;

    private final OrderService orderService;

    @GetMapping({"/", "/items"})
    public String getItemsPage(@RequestParam(required = false, defaultValue = "") String search,
                               @RequestParam(required = false, defaultValue = "NO") Sorter sort,
                               @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                               @RequestParam(required = false, defaultValue = "5") Integer pageSize,
                               Model model) {

        ItemsPage itemsPage = itemService.getItemsPage(search, sort, pageNumber, pageSize);

        // Добавляем атрибуты модели
        model.addAttribute("items", itemsPage.itemsRows());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", itemsPage.paging());

        // Возвращаем шаблон
        return "items";

    }

    @PostMapping({"/items"})
    public String addToCart(@RequestParam("id") Long itemId,
                            @RequestParam(required = false, defaultValue = "") String search,
                            @RequestParam(required = false, defaultValue = "NO") String sort,
                            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
                            @RequestParam Action action,
                            RedirectAttributes redirectAttributes) {

        itemService.actionWithItem(itemId, action);

        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);

        return "redirect:/items";
    }

    @GetMapping({"/items/{id}"})
    public String getItemPage(@PathVariable("id") Long itemId,
                              Model model) {

        ItemDto item = itemService.findItemById(itemId);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping({"/items/{id}"})
    public String addToCart(@PathVariable("id") Long itemId,
                            @RequestParam Action action,
                            Model model) {
        itemService.actionWithItem(itemId, action);
        ItemDto item = itemService.findItemById(itemId);
        model.addAttribute("item", item);
        return "item";
    }

    @GetMapping("/cart/items")
    public String getCartItemsPage(Model model) {
        List<ItemDto> items = itemService.findItemsDtoByCountGreaterThanZero();

        long totalSum = itemService.getTotalSum();

        model.addAttribute("items", items);
        model.addAttribute("total", totalSum);
        return "cart";
    }

    @PostMapping("/cart/items")
    public String modifyCartItem(@RequestParam("id") Long itemId,
                                 @RequestParam Action action) {
        itemService.actionWithItem(itemId, action);
        return "redirect:/cart/items";
    }

    @GetMapping("/orders")
    private String getOrdersPage(Model model) {
        List<OrderDto> orders = orderService.findAllOrders();
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrderPage(@PathVariable("id") Long orderId,
                               @RequestParam(required = false, defaultValue = "false") Boolean newOrder,
                               Model model) {

        OrderDto order = orderService.findOrderById(orderId);
        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @PostMapping("/buy")
    public String booking(RedirectAttributes redirectAttributes){
        OrderDto order = orderService.createOrder();

        redirectAttributes.addAttribute("id", order.id());
        redirectAttributes.addAttribute("newOrder", true);
        return "redirect:/orders/{id}";
    }

}
