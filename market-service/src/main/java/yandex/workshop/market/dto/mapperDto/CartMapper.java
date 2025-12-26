package yandex.workshop.market.dto.mapperDto;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import yandex.workshop.market.dto.CartDto;
import yandex.workshop.market.dto.CartItemDto;
import yandex.workshop.market.entity.Cart;

@Mapper(componentModel = "spring")
public interface CartMapper {

    CartMapper INSTANCE = Mappers.getMapper(CartMapper.class);

    @Mapping(target = "items", source = "items")
    @Mapping(target = "total", source = "cart.totalPrice")
    CartDto toDto(Cart cart, List<CartItemDto> items);
}

