package yandex.workshop.market.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.dto.mapperDto.OrderDtoMapper;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    private final ItemService itemService;

    public List<OrderDto> findAllOrders() {

        return orderRepository.findAll()
            .stream()
            .map(OrderDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());
    }


    @Transactional
    public OrderDto createOrder() {
        Order order = new Order();
        List<Item> itemsInCart = itemService.findItemsByCountGreaterThanZero();

        List<OrderItem> orderItems = new ArrayList<>();
        for (Item item : itemsInCart) {
            orderItems.add(new OrderItem(null, item, order, item.getCount()));
            // обнуляем корзину после покупки
            itemService.actionWithItem(item.getId(), Action.DELETE);
        }

        order.setItems(orderItems);

        order.setTotalSum(itemService.getTotalSum());
        return OrderDtoMapper.INSTANCE.toDto(orderRepository.save(order));
    }



    public OrderDto findOrderById(Long orderId) {

        return OrderDtoMapper.INSTANCE.toDto(orderRepository.findById(orderId).orElseThrow(() ->
            new NoSuchElementException("Order with id " + orderId + " not found")));
    }
}
