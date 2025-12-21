package yandex.workshop.market.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.Action;
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

    @Cacheable(value = "items", key = "#itemId")
    public Mono<ItemDto> findItemById(Long itemId) {
        return itemRepository.findById(itemId)
            .switchIfEmpty(Mono.error(new NoSuchElementException("Товар с id " + itemId + " не найден")))
            .map(ItemMapper.INSTANCE::toDto);
    }

    @CachePut(value = "items", key = "#itemId")
    @CacheEvict(
        value = { "itemsInCart", "itemsInCartTotal" },
        allEntries = true
    )
    public Mono<ItemDto> actionWithItem(Long itemId, Action action) {
        return switch (action) {
            case PLUS -> increaseItemQuantity(itemId).map(ItemMapper.INSTANCE::toDto);
            case MINUS -> decreaseItemQuantity(itemId).map(ItemMapper.INSTANCE::toDto);
            case DELETE -> resetItemQuantity(itemId).map(ItemMapper.INSTANCE::toDto);
        };
    }

    private Mono<Item> increaseItemQuantity(Long itemId) {
        return itemRepository.increaseItemCount(itemId);
    }

    private Mono<Item> decreaseItemQuantity(Long itemId) {
        return itemRepository.reduceItemCount(itemId);
    }

    private Mono<Item> resetItemQuantity(Long itemId) {
        return itemRepository.resetItemCount(itemId);

    }

    @Cacheable(value = "itemsInCart")
    public Flux<ItemDto> findItemsDtoByCountGreaterThanZero() {
        return findItemsByCountGreaterThanZero().map(ItemMapper.INSTANCE::toDto);
    }

    public Flux<Item> findItemsByCountGreaterThanZero() {
        return itemRepository.findItemsByCountGreaterThan(0);
    }

    @Cacheable(value = "itemsInCartTotal")
    public Mono<Long> getTotalSum() {
        return findItemsByCountGreaterThanZero()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .map(BigDecimal::longValue);
    }

    @Cacheable(value = "itemList", key = "'page:' + #pageNumber + ':size:' + #pageSize + ':search:' + #search + ':sort:' + #sort")
    public Mono<ItemsPageDto> getItemsPage(String search,
                                     Sorter sort,
                                     Integer pageNumber,
                                     Integer pageSize
    ) {

        return itemRepository.findAll()
            .filter(item -> {
                if (search.isBlank()) {
                    return true;
                }
                String query = search.toLowerCase();
                return item.getTitle().toLowerCase().contains(query)
                    || item.getDescription().toLowerCase().contains(query);
            }) // вынести в отдельный метод фильтрации getComparator(sort)
            .sort((item1, item2) -> {
                switch (sort) {
                    case ALPHA:
                        return item1.getTitle().compareTo(item2.getTitle());
                    case PRICE:
                        return item1.getPrice().compareTo(item2.getPrice());
                    case NO:
                    default:
                        return 0;
                }
            })
            .collectList()
            // вынести в отдельный метод buildPage(items, pageNumber, pageSize)
            .map(items -> {
                int totalItems = items.size();
                int fromIndex = Math.max((pageNumber - 1) * pageSize, 0);
                int toIndex = Math.min(fromIndex + pageSize, totalItems);

                List<Item> pageItems = fromIndex < totalItems ? items.subList(fromIndex, toIndex) : Collections.emptyList();

                boolean hasPrevious = pageNumber > 1;
                boolean hasNext = toIndex < totalItems;

                PagingDto pagingDto = new PagingDto(pageSize, pageNumber, hasPrevious, hasNext);

                // Формируем строки по 3 товара
                List<List<Item>> itemsRows = new ArrayList<>();
                for (int i = 0; i < pageItems.size(); i += 3) {
                    List<Item> row = new ArrayList<>();
                    for (int j = i; j < i + 3; j++) {
                        if (j < pageItems.size()) {
                            row.add(pageItems.get(j));
                        } else {
                            row.add(new Item(-1L, "", "", "", BigDecimal.ZERO, 0)); // заглушка
                        }
                    }
                    itemsRows.add(row);
                }

                return new ItemsPageDto(itemsRows, pagingDto);
            });

    }
}
