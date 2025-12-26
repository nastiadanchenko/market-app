package yandex.workshop.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yandex.workshop.market.dto.Action;
import yandex.workshop.market.dto.CartDto;
import yandex.workshop.market.dto.CartItemDto;
import yandex.workshop.market.dto.mapperDto.CartItemMapper;
import yandex.workshop.market.dto.mapperDto.CartMapper;
import yandex.workshop.market.dto.mapperDto.ItemMapper;
import yandex.workshop.market.entity.Cart;
import yandex.workshop.market.entity.CartItem;
import yandex.workshop.market.repository.CartItemRepository;
import yandex.workshop.market.repository.CartRepository;
import yandex.workshop.market.repository.ItemRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final UserService userService;

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final ItemRepository itemRepository;


    /**
     * Получить корзину текущего авторизованного пользователя.
     * Если корзины нет — создать.
     */

    public Mono<Cart> getCurrentUserCart() {
        return userService.currentUser()
            .flatMap(user ->
                getCartByUserId(user.getId())

            );
    }

    public Mono<CartDto> getCurrentUserCartDto() {
        return getCurrentUserCart()
            .flatMap(cart -> {
                log.debug("Cart DTO for cart id: {}", cart.getId());
                return cartItemRepository.findByCartId(cart.getId())
                    .flatMap(cartItem ->
                        itemRepository.findById(cartItem.getItemId())
                            .map(item -> CartItemMapper.INSTANCE.toDto(cartItem,
                                ItemMapper.INSTANCE.toDto(item, cartItem.getCount())))
                    )
                    .collectList()
                    .map(items -> CartMapper.INSTANCE.toDto(cart, items));
            });
    }

    /**
     * Добавить товар в корзину текущего пользователя
     */
    @CacheEvict(value = "userCart", allEntries = true)
    public Mono<CartItem> addItem(Long itemId) {
        return
            userService.currentUser()
                .flatMap(users -> getCartByUserId(users.getId())
                    .flatMap(cart ->
                        cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId)
                            .flatMap(existing -> {
                                existing.setCount(existing.getCount() + 1);
                                return cartItemRepository.save(existing);
                            })
                            .switchIfEmpty(
                                cartItemRepository.save(
                                    new CartItem(null, cart.getId(), itemId, 1, Instant.now())
                                )
                            )
                            .flatMap(savedItem ->
                                recalculateTotalSum(cart).thenReturn(savedItem)
                            )
                    )
                );
    }

    @Cacheable(value = "userCart", key = "#userId")
    public Mono<Cart> getCartByUserId(Long userId) {
        return cartRepository
            .findByUserId(userId)
            .switchIfEmpty(createCart(userId));
    }

    public Mono<Map<Long, Integer>> getCurrentUserCartItemCounts() {
        return getCurrentUserCart()
            .flatMapMany(cart ->
                cartItemRepository.findByCartId(cart.getId())
            )
            .collectMap(
                CartItem::getItemId,
                CartItem::getCount
            );
    }


    public Mono<?> actionWithItem(Long itemId, Action action) {
        return switch (action) {
            case PLUS -> addItem(itemId);
            case MINUS -> decreaseItem(itemId);
            case DELETE -> removeItem(itemId);
        };
    }

    /**
     * Уменьшить количество товара в корзине
     */
    @CacheEvict(
        value = { "cartItems", "cartTotal", "userCart" },
        allEntries = true
    )
    public Mono<Void> decreaseItem(Long itemId) {
        return getCurrentUserCart()
            .flatMap(cart ->
                cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId)
                    .flatMap(item -> {
                        if (item.getCount() <= 1) {
                            return cartItemRepository.delete(item);
                        }
                        item.setCount(item.getCount() - 1);
                        return cartItemRepository.save(item).then();
                    })
                    .then(recalculateTotalSum(cart))
            )
            .then();
    }


    /**
     * Полностью удалить товар из корзины
     */
    @CacheEvict(
        value = {"cartItems", "cartTotal", "userCart"}
    )
    public Mono<Void> removeItem(Long itemId) {
        return getCurrentUserCart()
            .flatMap(cart ->
                cartItemRepository.deleteByCartIdAndItemId(cart.getId(), itemId)
                    .then(recalculateTotalSum(cart))
            )
            .then();
    }

    /**
     * Очистить корзину (используется после успешной покупки)
     */
    @CacheEvict(
        value = {"cartItems", "cartTotal", "userCart"}
    )
    public Mono<Void> clearCart(Cart cart) {
        return cartItemRepository.deleteAllByCartId(cart.getId())
            .then(cartRepository.findById(cart.getId())
                .flatMap(c -> {
                    c.setTotalPrice(BigDecimal.ZERO);
                    c.setUpdatedAt(Instant.now());
                    return cartRepository.save(c);
                }))
            .then();
    }

    /**
     * Пересчёт totalSum корзины текущего пользователя
     */
    @Cacheable(value = "cartTotal", key = "#cart.id")
    public Mono<Cart> recalculateTotalSum(Cart cart) {
        return cartItemRepository.findByCartId(cart.getId())
            .flatMap(cartItem ->
                itemRepository.findById(cartItem.getItemId())
                    .map(item ->
                        item.getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getCount()))
                    )
            )
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .flatMap(totalSum -> {
                cart.setTotalPrice(totalSum);
                cart.setUpdatedAt(Instant.now());
                return cartRepository.save(cart);
            });
    }

    private Mono<Cart> createCart(Long userId) {
        Instant now = Instant.now();
        return cartRepository.save(new Cart(null, userId, now, now, BigDecimal.ZERO));
    }

    public Flux<CartItemDto> getCartItemsDto(Long cartId) {
        return cartItemRepository.findByCartId(cartId)
            .flatMap(cartItem ->
                itemRepository.findById(cartItem.getItemId())
                    .map(item ->
                        CartItemMapper.INSTANCE.toDto(cartItem, ItemMapper.INSTANCE.toDto(item)))
            );

    }

    public Flux<CartItem> getCartItems(Long cartId) {
        return cartItemRepository.findByCartId(cartId);


    }


    public Mono<Map<Long, Integer>> getCartItemCountsIfAuthenticated() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .filter(auth -> auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()))
            .flatMap(auth -> getCurrentUserCartItemCounts())
            .onErrorResume(ex -> Mono.just(Map.of()))
            .defaultIfEmpty(Map.of());
    }



}