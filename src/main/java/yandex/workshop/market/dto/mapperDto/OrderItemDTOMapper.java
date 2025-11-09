package yandex.workshop.market.dto.mapperDto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import yandex.workshop.market.dto.OrderItemDto;
import yandex.workshop.market.entity.OrderItem;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OrderItemDTOMapper {

    @Mapping(source = "item", target = "item")
    OrderItemDto toDto(OrderItem orderItem);

    @Mapping(source = "item", target = "item")
    OrderItem toEntity(OrderItemDto dto);
}
