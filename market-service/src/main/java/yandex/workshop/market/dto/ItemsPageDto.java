package yandex.workshop.market.dto;

import java.util.List;

public record ItemsPageDto(List<List<ItemDto>> itemsRows, PagingDto paging) {
}
