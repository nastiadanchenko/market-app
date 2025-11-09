package yandex.workshop.market.dto.mapperDto;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderDtoMapper {

    OrderDtoMapper INSTANCE = Mappers.getMapper(OrderDtoMapper.class);

    @Mapping(source = "items", target = "items")
    Order toEntity(OrderDto orderDto);

    @Mapping(source = "items", target = "items")
    OrderDto toDto(Order order);

    default List<ItemDto> mapOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null) return List.of();
        return orderItems.stream()
            .map(oi -> new ItemDto(
                oi.getItem().getId(),
                oi.getItem().getTitle(),
                oi.getItem().getPrice() != null ? oi.getItem().getPrice() : BigDecimal.ZERO,
                oi.getCount() != null ? oi.getCount() : 0,
                oi.getItem().getImgPath(),
                oi.getItem().getDescription()
            ))
            .toList();
    }

}
