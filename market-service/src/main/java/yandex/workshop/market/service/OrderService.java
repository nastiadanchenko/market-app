package yandex.workshop.market.service;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.ItemDto;
import yandex.workshop.market.dto.OrderDto;
import yandex.workshop.market.entity.Cart;
import yandex.workshop.market.entity.Order;
import yandex.workshop.market.entity.OrderItem;
import yandex.workshop.market.repository.OrderItemRepository;
import yandex.workshop.market.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ItemService itemService;
    private final TransactionalOperator transactionalOperator;
    private final UserService userService;

    /**
     * Получить все заказы текущего пользователя
     */
    public Flux<OrderDto> findAllOrdersForCurrentUser() {
        return userService.currentUser()
            .flatMapMany(user ->
                orderRepository.findAllByUserId(user.getId())
                    .flatMap(this::buildOrderDto)
            );
    }

    /**
     * Создание заказа из корзины текущего пользователя
     */
    public Mono<OrderDto> createOrder(Cart cart) {
        return transactionalOperator.transactional(
            cartService.getCartItems(cart.getId())
                .collectList()
                .filter(items -> !items.isEmpty())
                .switchIfEmpty(
                    Mono.error(new IllegalStateException("Корзина пуста"))
                )
                .flatMap(cartItems -> {

                    Order order = new Order();
                    order.setUserId(cart.getUserId());
                    order.setTotalSum(cart.getTotalPrice());

                    return orderRepository.save(order)
                        .flatMap(savedOrder -> {

                            List<OrderItem> orderItems = cartItems.stream()
                                .map(ci ->
                                    new OrderItem(
                                        null,
                                        ci.getItemId(),
                                        savedOrder.getId(),
                                        ci.getCount()
                                    )
                                )
                                .toList();

                            return orderItemRepository.saveAll(orderItems)
                                .then(cartService.clearCart(cart))
                                .thenReturn(savedOrder);
                        });
                })
                .flatMap(this::buildOrderDto)
        );
    }

    /**
     * Получить заказ по id
     */
    public Mono<OrderDto> findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(
                Mono.error(new NoSuchElementException("Order with id " + orderId + " not found"))
            )
            .flatMap(this::buildOrderDto);
    }

    /**
     * Сборка OrderDto
     */
    public Mono<OrderDto> buildOrderDto(Order order) {
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
                tuple.getT1().getTotalSum().longValue()
            )
        );
    }
}
