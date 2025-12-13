package yandex.workshop.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
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
        @DisplayName("findItemById — товар найден")
        void testFindItemById() {
            when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));

            Mono<ItemDto> result = itemService.findItemById(1L);

            StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(item1.getId());
                    assertThat(dto.title()).isEqualTo(item1.getTitle());
                })
                .verifyComplete();

            verify(itemRepository).findById(1L);
        }

        @Test
        @DisplayName("findItemById — товар НЕ найден → NoSuchElementException")
        void testFindItemByIdNotFound() {
            when(itemRepository.findById(100L)).thenReturn(Mono.empty());

            StepVerifier.create(itemService.findItemById(100L))
                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                    throwable.getMessage().contains("100"))
                .verify();

            verify(itemRepository).findById(100L);
        }

        @Test
        @DisplayName("findItemsByCountGreaterThanZero — должен возвращать только товары с count > 0")
        void testFindItemsByCountGreaterThanZero() {
            when(itemRepository.findItemsByCountGreaterThan(0))
                .thenReturn(Flux.just(item1, item3));

            Flux<Item> result = itemService.findItemsByCountGreaterThanZero();

            StepVerifier.create(result)
                .expectNext(item1, item3)
                .verifyComplete();

            verify(itemRepository).findItemsByCountGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Тестирование методов изменения количества")
    class ActionMethods {

        @Test
        @DisplayName("actionWithItem(PLUS) — вызывает increaseItemCount(id)")
        void testActionPlus() {
            when(itemRepository.increaseItemCount(1L)).thenReturn(Mono.just(item1));

            StepVerifier.create(itemService.actionWithItem(1L, Action.PLUS))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(1L))
                .verifyComplete();

            verify(itemRepository).increaseItemCount(1L);
        }

        @Test
        @DisplayName("actionWithItem(MINUS) — вызывает reduceItemCount(id)")
        void testActionMinus() {
            when(itemRepository.reduceItemCount(2L)).thenReturn(Mono.just(item2));

            StepVerifier.create(itemService.actionWithItem(2L, Action.MINUS))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(2L))
                .verifyComplete();

            verify(itemRepository).reduceItemCount(2L);

        }

        @Test
        @DisplayName("actionWithItem(DELETE) — вызывает resetItemCount(id)")
        void testActionDelete() {
            when(itemRepository.resetItemCount(3L)).thenReturn(Mono.just(item3));

            StepVerifier.create(itemService.actionWithItem(3L, Action.DELETE))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(3L))
                .verifyComplete();

            verify(itemRepository).resetItemCount(3L);

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

        when(itemRepository.findItemsByCountGreaterThan(0))
            .thenReturn(Flux.just(item1, item3));

        StepVerifier.create(itemService.getTotalSum())
            .expectNext(425L)
            .verifyComplete();

        verify(itemRepository).findItemsByCountGreaterThan(0);
    }

    @Nested
    @DisplayName("Тестирование пагинации и сортировки")
    class PaginationTests {

        @Test
        @DisplayName("getItemsPage — правильная фильтрация и пагинация")
        void testPaginationBasic() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item1, item2, item3));

            Mono<ItemsPageDto> page = itemService.getItemsPage("", Sorter.NO, 1, 3);

            StepVerifier.create(page)
                .assertNext(dto -> {
                    assertThat(dto.itemsRows()).hasSize(1);
                    assertThat(dto.itemsRows().get(0)).containsExactly(item1, item2, item3);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("getItemsPage — фильтрация по поиску")
        void testSearchFilter() {
            when(itemRepository.findAll()).thenReturn(Flux.just(item1, item2, item3));

            Mono<ItemsPageDto> page = itemService.getItemsPage(
                "Кру", // найдёт "Кружка"
                Sorter.NO,
                1,
                10
            );

            StepVerifier.create(page)
                .assertNext(dto -> {
                    assertThat(dto.itemsRows()).hasSize(1);
                    assertThat(dto.itemsRows().get(0).get(0)).isEqualTo(item2);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("getItemsPage — сортировка по алфавиту")
        void testSortingAlpha() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item3, item1, item2));

            Mono<ItemsPageDto> page = itemService.getItemsPage("", Sorter.ALPHA, 1, 10);

            StepVerifier.create(page)
                .assertNext(dto -> {
                    List<Item> flat = dto.itemsRows().stream().flatMap(List::stream).toList();
                    assertThat(flat.get(0).getTitle()).isEqualTo("Бутылка");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("getItemsPage — сортировка по цене")
        void testSortingPrice() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item3, item1, item2));

            Mono<ItemsPageDto> page = itemService.getItemsPage("", Sorter.PRICE, 1, 10);

            StepVerifier.create(page)
                .assertNext(dto -> {
                    List<Item> flat = dto.itemsRows().stream().flatMap(List::stream).toList();
                    assertThat(flat.get(0).getPrice()).isEqualByComparingTo("50.00");
                    assertThat(flat.get(1).getPrice()).isEqualByComparingTo("75.00");
                    assertThat(flat.get(2).getPrice()).isEqualByComparingTo("100.00");
                })
                .verifyComplete();
        }
    }
}

