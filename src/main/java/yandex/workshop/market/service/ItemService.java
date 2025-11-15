package yandex.workshop.market.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<Item> findAllItems() {
        return itemRepository.findAll();

    }

    public ItemDto findItemById(Long itemId) {
        return ItemMapper.INSTANCE.toDto(itemRepository.findById(itemId).orElseThrow(() ->
            new NoSuchElementException("Товар с id " + itemId + " не найден")));
    }


    @Transactional
    public void actionWithItem(Long itemId, Action action) {
        switch (action) {
            case PLUS -> increaseItemQuantity(itemId);
            case MINUS -> decreaseItemQuantity(itemId);
            case DELETE -> resetItemQuantity(itemId);
        }

    }

    private void increaseItemQuantity(Long itemId) {
        itemRepository.increaseItemCount(itemId);
    }

    private void decreaseItemQuantity(Long itemId) {
        itemRepository.reduceItemCount(itemId);
    }

    private void resetItemQuantity(Long itemId) {
        itemRepository.resetItemCount(itemId);
    }

    public List<ItemDto> findItemsDtoByCountGreaterThanZero() {

        return findItemsByCountGreaterThanZero()
            .stream()
            .map(ItemMapper.INSTANCE::toDto)
            .collect(Collectors.toList());
    }

    public List<Item> findItemsByCountGreaterThanZero() {

        return itemRepository.findByCountGreaterThan(0);
    }

    public Long getTotalSum() {
        List<Item> items = itemRepository.findByCountGreaterThan(0);
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add).longValue();
    }

    public ItemsPageDto getItemsPage(String search,
                                     Sorter sort,
                                     Integer pageNumber,
                                     Integer pageSize
    ) {

        List<Item> items = findAllItems();
        // Фильтрация по поисковому запросу
        if (!search.isBlank()) {
            String query = search.toLowerCase();
            items = items.stream()
                .filter(item -> item.getTitle().toLowerCase().contains(query)
                    || item.getDescription().toLowerCase().contains(query))
                .collect(Collectors.toList());
        }

        // Сортировка
        switch (sort) {
            case ALPHA -> items.sort(Comparator.comparing(Item::getTitle));
            case PRICE -> items.sort(Comparator.comparing(Item::getPrice));
            case NO -> {
            } // ничего не делаем
        }

        // Пагинация
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
    }
}
