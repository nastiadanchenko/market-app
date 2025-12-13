package yandex.workshop.market.service;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.OrderItemRepository;
import yandex.workshop.market.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    private final ItemService itemService;

    private final OrderItemRepository orderItemRepository;

    private final TransactionalOperator transactionalOperator;

    public Flux<OrderDto> findAllOrders() {
        return orderRepository.findAll()
            .flatMap(this::getOrderDto);
    }

    public Mono<OrderDto> createOrder() {

        return transactionalOperator.transactional(itemService.findItemsByCountGreaterThanZero()
            .collectList() // получили товары
            .flatMap(itemsInCart -> {

                Order order = new Order();

                // посчитать total
                return itemService.getTotalSum()
                    .flatMap(totalSum -> {
                        order.setTotalSum(totalSum);

                        // сохранить заказ
                        return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                // создать orderItems
                                List<OrderItem> list = itemsInCart.stream()
                                    .map(item -> new OrderItem(null, item.getId(), savedOrder.getId(), item.getCount()))
                                    .toList();

                                return orderItemRepository.saveAll(list)
                                    .collectList()
                                    .flatMap(savedItems -> {
                                        // очистить корзину
                                        return Flux.fromIterable(itemsInCart)
                                            .flatMap(item -> itemService.actionWithItem(item.getId(), Action.DELETE))
                                            .then(Mono.just(savedOrder));
                                    });
                            });
                    });
            })
            .flatMap(this::getOrderDto)
        );
    }


    public Mono<OrderDto> findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new NoSuchElementException("Order with id " + orderId + " not found")))
            .flatMap(this::getOrderDto);
    }


    public Mono<OrderDto> getOrderDto(Order order) {
        Flux<OrderItem> orderItemsFlux = orderItemRepository.findByOrderId(order.getId());

        return Mono.just(order).zipWith(
            orderItemsFlux
                .flatMap(oi ->
                    itemService.findItemById(oi.getItemId())
                        .map(itemDto ->
                            new ItemDto(
                                itemDto.id(),
                                itemDto.title(),
                                itemDto.price(),
                                oi.getCount(),
                                itemDto.imgPath(),
                                itemDto.description()
                            )
                        )
                )
                .collectList()
        ).map(tuple ->
            new OrderDto(
                tuple.getT1().getId(),
                tuple.getT2(),
                tuple.getT1().getTotalSum()
            )
        );
    }
}
