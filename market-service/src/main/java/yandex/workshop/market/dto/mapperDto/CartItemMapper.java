package yandex.workshop.market.dto.mapperDto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import yandex.workshop.market.dto.CartItemDto;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.entity.CartItem;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    CartItemMapper INSTANCE = Mappers.getMapper(CartItemMapper.class);

    @Mapping(target = "item", source = "item")
    @Mapping(target = "count", source = "cartItem.count")
    @Mapping(
        target = "sum",
        expression = "java(item.price().multiply(BigDecimal.valueOf(cartItem.getCount())))"
    )
    CartItemDto toDto(CartItem cartItem, ItemDto item);
}
