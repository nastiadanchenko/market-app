package yandex.workshop.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.Sorter;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.repository.ItemRepository;
import yandex.workshop.market.service.CartService;
import yandex.workshop.market.service.ItemService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
class ItemServiceTest {

    @MockitoBean
    private ItemRepository itemRepository;

    @MockitoBean
    private CartService cartService;

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
            when(itemRepository.findById(1L))
                .thenReturn(Mono.just(item1));

            when(cartService.getCurrentUserCartItemCounts())
                .thenReturn(Mono.just(Map.of(1L, 5)));

            StepVerifier.create(itemService.findItemById(1L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.title()).isEqualTo("Бутылка");
                    assertThat(dto.count()).isEqualTo(5);
                })
                .verifyComplete();

            verify(itemRepository).findById(1L);
            verify(cartService).getCurrentUserCartItemCounts();

        }

        @Test
        @DisplayName("findItemById — товар НЕ найден → NoSuchElementException")
        void testFindItemByIdNotFound() {
            when(itemRepository.findById(100L))
                .thenReturn(Mono.empty());

            StepVerifier.create(itemService.findItemById(100L))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(NoSuchElementException.class);
                    assertThat(ex.getMessage()).contains("100");
                })
                .verify();

            verify(itemRepository).findById(100L);
            verifyNoInteractions(cartService);

        }

    }

    @Nested
    @DisplayName("Тестирование пагинации и сортировки")
    class PaginationTests {

        @Test
        @DisplayName("getItemsPage — без поиска и сортировки")
        void testPaginationBasic() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item1, item2, item3));

            when(cartService.getCartItemCountsIfAuthenticated())
                .thenReturn(Mono.just(Map.of(
                    1L, 2,
                    3L, 1
                )));

            StepVerifier.create(itemService.getItemsPage("", Sorter.NO, 1, 5))
                .assertNext(dto -> {
                    assertThat(dto.itemsRows()).hasSize(1);

                    List<ItemDto> flat =
                        dto.itemsRows().get(0);

                    assertThat(flat.get(0).id()).isEqualTo(1L);
                    assertThat(flat.get(0).count()).isEqualTo(2);

                    assertThat(flat.get(2).id()).isEqualTo(3L);
                    assertThat(flat.get(2).count()).isEqualTo(1);
                })
                .verifyComplete();

            verify(itemRepository).findAll();
            verify(cartService).getCartItemCountsIfAuthenticated();
        }

        @Test
        @DisplayName("getItemsPage — фильтрация по поиску")
        void testSearchFilter() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item1, item2, item3));

            when(cartService.getCartItemCountsIfAuthenticated())
                .thenReturn(Mono.just(Map.of()));

            StepVerifier.create(
                    itemService.getItemsPage("Кру", Sorter.NO, 1, 10)
                )
                .assertNext(dto -> {
                    List<String> titles =
                        dto.itemsRows().stream()
                            .flatMap(List::stream)
                            .map(ItemDto::title)
                            .filter(t -> !t.isBlank())
                            .toList();

                    assertThat(titles)
                        .containsExactly("Кружка");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("getItemsPage — сортировка по алфавиту")
        void testSortingAlpha() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item3, item1, item2));

            when(cartService.getCartItemCountsIfAuthenticated())
                .thenReturn(Mono.just(Map.of()));

            StepVerifier.create(
                    itemService.getItemsPage("", Sorter.ALPHA, 1, 10)
                )
                .assertNext(dto -> {
                    List<String> titles =
                        dto.itemsRows().stream()
                            .flatMap(List::stream)
                            .map(ItemDto::title)
                            .toList();

                    assertThat(titles)
                        .startsWith("Бутылка");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("getItemsPage — сортировка по цене")
        void testSortingPrice() {
            when(itemRepository.findAll())
                .thenReturn(Flux.just(item3, item1, item2));

            when(cartService.getCartItemCountsIfAuthenticated())
                .thenReturn(Mono.just(Map.of()));

            StepVerifier.create(
                    itemService.getItemsPage("", Sorter.PRICE, 1, 10)
                )
                .assertNext(dto -> {
                    List<BigDecimal> prices =
                        dto.itemsRows().stream()
                            .flatMap(List::stream)
                            .map(ItemDto::price)
                            .toList();

                    assertThat(prices)
                        .isSorted();
                })
                .verifyComplete();
        }
    }
}

