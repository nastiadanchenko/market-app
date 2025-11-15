package yandex.workshop.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.ItemsPageDto;
import yandex.workshop.market.dto.Sorter;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.repository.ItemRepository;
import yandex.workshop.market.service.ItemService;

@SpringBootTest
class ItemServiceTest {

    @MockitoBean
    private ItemRepository itemRepository;

    @Autowired
    private ItemService itemService;


    private Item item1;
    private Item item2;
    private Item item3;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        item1 = new Item(1L, "Бутылка", "Пластиковая", "", new BigDecimal("100.00"), 2);
        item2 = new Item(2L, "Кружка", "Керамическая", "", new BigDecimal("50.00"), 0);
        item3 = new Item(3L, "Тарелка", "Большая", "", new BigDecimal("75.00"), 3);
    }
    @Nested
    @DisplayName("Тестирование методов поиска")
    class FindMethods {

        @Test
        @DisplayName("findAllItems — должен вернуть все товары")
        void testFindAllItems() {
            when(itemRepository.findAll()).thenReturn(List.of(item1, item2, item3));

            List<Item> result = itemService.findAllItems();

            assertThat(result).containsExactly(item1, item2, item3);
            verify(itemRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("findItemById — товар найден")
        void testFindItemById() {
            when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));

            ItemDto dto = itemService.findItemById(1L);

            assertThat(dto.id()).isEqualTo(item1.getId());
            assertThat(dto.title()).isEqualTo(item1.getTitle());
            verify(itemRepository).findById(1L);
        }

        @Test
        @DisplayName("findItemById — товар НЕ найден → NoSuchElementException")
        void testFindItemByIdNotFound() {
            when(itemRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.findItemById(100L))
                .isInstanceOf(NoSuchElementException.class);

            verify(itemRepository).findById(100L);
        }

        @Test
        @DisplayName("findItemsByCountGreaterThanZero — должен возвращать только товары с count > 0")
        void testFindItemsByCountGreaterThanZero() {
            when(itemRepository.findByCountGreaterThan(0))
                .thenReturn(List.of(item1, item3));

            List<Item> result = itemService.findItemsByCountGreaterThanZero();

            assertThat(result).containsExactly(item1, item3);
            verify(itemRepository).findByCountGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Тестирование методов изменения количества")
    class ActionMethods {

        @Test
        @DisplayName("actionWithItem(PLUS) — вызывает increaseItemCount(id)")
        void testActionPlus() {
            itemService.actionWithItem(1L, Action.PLUS);

            verify(itemRepository).increaseItemCount(1L);
            verify(itemRepository, never()).reduceItemCount(any());
            verify(itemRepository, never()).resetItemCount(any());
        }

        @Test
        @DisplayName("actionWithItem(MINUS) — вызывает reduceItemCount(id)")
        void testActionMinus() {
            itemService.actionWithItem(2L, Action.MINUS);

            verify(itemRepository).reduceItemCount(2L);
            verify(itemRepository, never()).increaseItemCount(any());
            verify(itemRepository, never()).resetItemCount(any());
        }

        @Test
        @DisplayName("actionWithItem(DELETE) — вызывает resetItemCount(id)")
        void testActionDelete() {
            itemService.actionWithItem(3L, Action.DELETE);

            verify(itemRepository).resetItemCount(3L);
            verify(itemRepository, never()).increaseItemCount(any());
            verify(itemRepository, never()).reduceItemCount(any());
        }
    }

    @Test
    @DisplayName("getTotalSum — считает сумму правильно")
    void testGetTotalSum() {

        /*
         item1 → 2 * 100 = 200
         item3 → 3 * 75 = 225
         total = 425
        */

        when(itemRepository.findByCountGreaterThan(0))
            .thenReturn(List.of(item1, item3));

        Long sum = itemService.getTotalSum();

        assertThat(sum).isEqualTo(425L);
        verify(itemRepository).findByCountGreaterThan(0);
    }

    @Nested
    @DisplayName("Тестирование пагинации и сортировки")
    class PaginationTests {

        @Test
        @DisplayName("getItemsPage — правильная фильтрация и пагинация")
        void testPaginationBasic() {
            when(itemRepository.findAll()).thenReturn(List.of(item1, item2, item3));

            ItemsPageDto page = itemService.getItemsPage(
                "",
                Sorter.NO,
                1,
                2
            );

            // проверяем структуру
            assertThat(page.itemsRows()).hasSize(1);
            assertThat(page.itemsRows().get(0)).hasSize(3); // 3 товара в строке (1 пустой)

            verify(itemRepository).findAll();
        }

        @Test
        @DisplayName("getItemsPage — фильтрация по поиску")
        void testSearchFilter() {
            when(itemRepository.findAll()).thenReturn(List.of(item1, item2, item3));

            ItemsPageDto page = itemService.getItemsPage(
                "Кру", // найдёт "Кружка"
                Sorter.NO,
                1,
                10
            );

            Item result = page.itemsRows().get(0).get(0);

            assertThat(result.getTitle()).isEqualTo("Кружка");
        }

        @Test
        @DisplayName("getItemsPage — сортировка по алфавиту")
        void testSortingAlpha() {
            when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(item1, item3, item2)));

            ItemsPageDto page = itemService.getItemsPage("", Sorter.ALPHA, 1, 10);

            List<Item> flat = page.itemsRows().stream()
                .flatMap(List::stream)
                .toList();

            assertThat(flat.get(0).getTitle()).isEqualTo("Бутылка");
            assertThat(flat.get(1).getTitle()).isEqualTo("Кружка");
            assertThat(flat.get(2).getTitle()).isEqualTo("Тарелка");
        }

        @Test
        @DisplayName("getItemsPage — сортировка по цене")
        void testSortingPrice() {
            when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(item1, item3, item2)));

            ItemsPageDto page = itemService.getItemsPage("", Sorter.PRICE, 1, 10);

            List<Item> flat = page.itemsRows().stream()
                .flatMap(List::stream)
                .toList();

            assertThat(flat.get(0).getPrice()).isEqualTo(new BigDecimal("50.00"));
            assertThat(flat.get(1).getPrice()).isEqualTo(new BigDecimal("75.00"));
            assertThat(flat.get(2).getPrice()).isEqualTo(new BigDecimal("100.00"));
        }
    }
}

