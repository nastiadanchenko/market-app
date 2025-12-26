package yandex.workshop.market.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.ItemsPageDto;
import yandex.workshop.market.dto.PagingDto;
import yandex.workshop.market.dto.Sorter;
import yandex.workshop.market.dto.mapperDto.ItemMapper;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.repository.ItemRepository;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    private final CartService cartService;

    @Cacheable(value = "items", key = "#itemId")
    public Mono<ItemDto> findItemById(Long itemId) {
        return itemRepository.findById(itemId)
            .switchIfEmpty(Mono.error(new NoSuchElementException("Товар с id " + itemId + " не найден")))
            .flatMap(item ->
                cartService.getCurrentUserCartItemCounts()
                    .map(cartCounts ->
                        ItemMapper.INSTANCE.toDto(item, cartCounts.getOrDefault(item.getId(), 0))
                    )
            );
    }

    /**
     * Витрина товаров с поиском, сортировкой и пагинацией
     */
    @Cacheable(value = "itemList", key = "'page:' + #pageNumber + ':size:' + #pageSize + ':search:' + #search + ':sort:' + #sort")
    public Mono<ItemsPageDto> getItemsPage(String search,
                                     Sorter sort,
                                     Integer pageNumber,
                                     Integer pageSize
    ) {

        Mono<Map<Long, Integer>> cartCounts =
            cartService.getCartItemCountsIfAuthenticated();

        Mono<List<Item>> itemsMono = itemRepository.findAll()
            .filter(item -> {
                if (search.isBlank()) {
                    return true;
                }
                String q = search.toLowerCase();
                return item.getTitle().toLowerCase().contains(q)
                    || item.getDescription().toLowerCase().contains(q);
            })
            .sort(getComparator(sort))
            .collectList();

        return Mono.zip(itemsMono, cartCounts)
            .map(tuple -> {
                List<Item> items = tuple.getT1();
                Map<Long, Integer> counts = tuple.getT2();

                List<ItemDto> enriched = items.stream()
                    .map(item ->
                        ItemMapper.INSTANCE.toDto(
                            item,
                            counts.getOrDefault(item.getId(), 0))
                    )
                    .toList();

                return buildPage(enriched, pageNumber, pageSize);
            });

    }

    private ItemsPageDto buildPage(List<ItemDto> items, int pageNumber, int pageSize) {

        int totalItems = items.size();
        int fromIndex = Math.max((pageNumber - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        List<ItemDto> pageItems =
            fromIndex < totalItems ? items.subList(fromIndex, toIndex) : Collections.emptyList();

        boolean hasPrevious = pageNumber > 1;
        boolean hasNext = toIndex < totalItems;

        PagingDto pagingDto =
            new PagingDto(pageSize, pageNumber, hasPrevious, hasNext);

        List<List<ItemDto>> itemsRows = new ArrayList<>();
        for (int i = 0; i < pageItems.size(); i += 3) {
            List<ItemDto> row = new ArrayList<>();
            for (int j = i; j < i + 3; j++) {
                row.add(j < pageItems.size()
                    ? pageItems.get(j)
                    : new ItemDto(-1L, "", BigDecimal.ZERO ,0 ,"", "")
                );
            }
            itemsRows.add(row);
        }

        return new ItemsPageDto(itemsRows, pagingDto);
    }

    private Comparator<Item> getComparator(Sorter sort) {
        return switch (sort) {
            case ALPHA ->
                Comparator.comparing(Item::getTitle, String::compareToIgnoreCase);
            case PRICE ->
                Comparator.comparing(Item::getPrice);
            case NO ->
                (o1, o2) -> 0;
            default -> (o1, o2) -> 0;
        };
    }
}
