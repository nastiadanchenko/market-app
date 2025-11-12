package yandex.workshop.market;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.ItemsPageDto;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.PagingDto;
import yandex.workshop.market.dto.mapperDto.ItemMapper;
import yandex.workshop.market.service.ItemService;
import yandex.workshop.market.service.OrderService;

@WebMvcTest
public class ApiControllerMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private OrderService orderService;

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

        when(itemService.getItemsPage(any(), any(), anyInt(), anyInt())).thenReturn(page);


        mockMvc.perform(get("/items"))
            .andExpect(status().isOk())
            .andExpect(view().name("items"))
            .andExpect(model().attributeExists("items"))
            .andExpect(model().attributeExists("search"))
            .andExpect(model().attributeExists("sort"))
            .andExpect(model().attributeExists("paging"));
    }

    @Test
    @DisplayName("POST /items - должен добавлять товар в корзину и перенаправлять на /items")
    void shouldAddItemToCart() throws Exception {
        mockMvc.perform(post("/items")
                .param("id", "1")
                .param("action", "PLUS"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));

        Mockito.verify(itemService).actionWithItem(anyLong(), any());
    }

    @Test
    @DisplayName("GET /items/{id} - должен возвращать страницу с деталями товара")
    void shouldReturnItemDetails() throws Exception {

        when(itemService.findItemById(itemDto.id())).thenReturn(itemDto);

        mockMvc.perform(get("/items/{id}", itemDto.id()))
            .andExpect(status().isOk())
            .andExpect(view().name("item"))
            .andExpect(model().attributeExists("item"));
    }

    @Test
    @DisplayName("POST /items/{id} - должен изменять количество товара в корзине и перенаправлять на /items/{id}")
    void shouldChangeItemQuantityInCart() throws Exception {
        Long itemId = itemDto.id();

        when(itemService.findItemById(itemId)).thenReturn(itemDto);

        mockMvc.perform(post("/items/{id}", itemId)
                .param("action", "MINUS"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("item", itemDto));

        Mockito.verify(itemService).actionWithItem(itemId, Action.MINUS);
    }


    @Test
    @DisplayName("GET /cart/items returns cart page")
    void shouldReturnCartItemsPage() throws Exception {
        ItemDto itemDto = new ItemDto(1L, "Title", BigDecimal.valueOf(100), 2, "/img.png", "Desc");
        when(itemService.findItemsDtoByCountGreaterThanZero()).thenReturn(List.of(itemDto));
        when(itemService.getTotalSum()).thenReturn(200L);

        mockMvc.perform(get("/cart/items"))
            .andExpect(status().isOk())
            .andExpect(view().name("cart"))
            .andExpect(model().attributeExists("items"))
            .andExpect(model().attributeExists("total"));
    }

    @Test
    @DisplayName("POST /cart/items modifies cart item and redirects")
    void shouldModifyCartItemAndRedirect() throws Exception {
        mockMvc.perform(post("/cart/items")
                .param("id", "1")
                .param("action", "PLUS"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart/items"));

        Mockito.verify(itemService).actionWithItem(1L, Action.PLUS);
    }

    @Test
    @DisplayName("GET /orders returns orders page")
    void shouldReturnOrdersPage() throws Exception {
        OrderDto orderDto = new OrderDto(1L, List.of(), 100L);
        when(orderService.findAllOrders()).thenReturn(List.of(orderDto));

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(view().name("orders"))
            .andExpect(model().attributeExists("orders"));
    }

    @Test
    @DisplayName("GET /orders/{id} returns order page")
    void shouldReturnOrderPage() throws Exception {
        OrderDto orderDto = new OrderDto(1L, List.of(), 100L);
        when(orderService.findOrderById(1L)).thenReturn(orderDto);

        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("order"))
            .andExpect(model().attributeExists("order"))
            .andExpect(model().attributeExists("newOrder"));
    }

    @Test
    @DisplayName("POST /buy creates order and redirects")
    void shouldCreateOrderAndRedirect() throws Exception {
        OrderDto orderDto = new OrderDto(1L, List.of(), 100L);
        when(orderService.createOrder()).thenReturn(orderDto);

        mockMvc.perform(post("/buy"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/1?newOrder=true"));
    }


}
