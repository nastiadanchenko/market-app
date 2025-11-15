package yandex.workshop.market.dto;

import java.util.List;
import yandex.workshop.market.entity.Item;

public record ItemsPageDto(List<List<Item>> itemsRows, PagingDto paging) {
}
