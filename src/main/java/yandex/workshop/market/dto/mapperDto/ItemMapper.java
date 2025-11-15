package yandex.workshop.market.dto.mapperDto;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.entity.Item;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemMapper  INSTANCE = Mappers.getMapper(ItemMapper.class);

    Item toEntity(ItemDto itemDto);

    ItemDto toDto(Item item);
}
