package yandex.workshop.market;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import yandex.workshop.market.entity.Cart;
import yandex.workshop.market.entity.CartItem;
import yandex.workshop.market.entity.Item;
import yandex.workshop.market.entity.Users;
import yandex.workshop.market.repository.CartItemRepository;
import yandex.workshop.market.repository.CartRepository;
import yandex.workshop.market.repository.ItemRepository;
import yandex.workshop.market.service.CartService;
import yandex.workshop.market.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
public class CartServiceTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private ItemRepository itemRepository;

    @Autowired
    private CartService cartService;

    private Users user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(10L);
        user.setName("user");
        user.setPassword("pass");

        cart = new Cart();
        cart.setId(1L);
        cart.setUserId(10L);
        cart.setTotalPrice(BigDecimal.ZERO);

        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.currentUser()).thenReturn(Mono.just(user));
    }

    @Test
    @DisplayName("getCurrentUserCart — возвращает существующую корзину")
    void getCurrentUserCart_existing() {

        when(userService.currentUser()).thenReturn(Mono.just(user));

        Cart cart = new Cart(1L, 10L, null, null, BigDecimal.ZERO);
        when(cartRepository.findByUserId(10L)).thenReturn(Mono.just(cart));

        when(cartRepository.save(any(Cart.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Создаем Authentication и SecurityContext
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass");
        SecurityContext context = new SecurityContextImpl(auth);

        StepVerifier.create(
                cartService.getCurrentUserCart()
                    .contextWrite(
                        ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))
            )
            .expectNext(cart)
            .verifyComplete();

        verify(cartRepository).findByUserId(10L);
        verify(cartRepository, never()).save(argThat(c -> c.getId() != null));
    }

    @Test
    @DisplayName("getCurrentUserCart — создаёт корзину если её нет")
    void getCurrentUserCart_create() {

        when(cartRepository.findByUserId(10L))
            .thenReturn(Mono.empty());

        when(cartRepository.save(any(Cart.class)))
            .thenAnswer(inv -> {
                Cart c = inv.getArgument(0);
                c.setId(1L);
                return Mono.just(c);
            });

        StepVerifier.create(cartService.getCurrentUserCart())
            .assertNext(c -> {
                assertThat(c.getUserId()).isEqualTo(10L);
                assertThat(c.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("addItem — добавляет новый CartItem")
    void addItem_newItem() {

        when(cartRepository.findByUserId(10L)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findByCartIdAndItemId(1L, 5L))
            .thenReturn(Mono.empty());

        when(cartItemRepository.save(any(CartItem.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(cartItemRepository.findByCartId(1L))
            .thenReturn(Flux.just(new CartItem(null, 1L, 5L, 1, Instant.now())));

        when(itemRepository.findById(5L))
            .thenReturn(Mono.just(
                new Item(5L, "Item", "", "", BigDecimal.valueOf(100), 1)
            ));

        when(cartRepository.save(any(Cart.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(cartService.addItem(5L))
            .assertNext(ci -> {
                assertThat(ci.getItemId()).isEqualTo(5L);
                assertThat(ci.getCount()).isEqualTo(1);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("addItem — увеличивает количество существующего товара")
    void addItem_existingItem() {

        CartItem existing = new CartItem(1L, 1L, 5L, 1, Instant.now());

        when(cartRepository.findByUserId(10L)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findByCartIdAndItemId(1L, 5L))
            .thenReturn(Mono.just(existing));

        when(cartItemRepository.save(any(CartItem.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(cartItemRepository.findByCartId(1L))
            .thenReturn(Flux.just(existing));

        when(itemRepository.findById(5L))
            .thenReturn(Mono.just(
                new Item(5L, "Item", "", "", BigDecimal.valueOf(100), 1)
            ));

        when(cartRepository.save(any(Cart.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(cartService.addItem(5L))
            .assertNext(ci -> assertThat(ci.getCount()).isEqualTo(2))
            .verifyComplete();
    }

    @Test
    @DisplayName("decreaseItem — уменьшает count если count > 1")
    void decreaseItem_reduce() {

        CartItem item = new CartItem(1L, 1L, 5L, 2, Instant.now());

        when(cartRepository.findByUserId(10L)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findByCartIdAndItemId(1L, 5L))
            .thenReturn(Mono.just(item));

        when(cartItemRepository.save(any()))
            .thenReturn(Mono.just(item));

        when(cartItemRepository.findByCartId(1L))
            .thenReturn(Flux.just(item));

        when(itemRepository.findById(5L))
            .thenReturn(Mono.just(
                new Item(5L, "Item", "", "", BigDecimal.valueOf(100), 1)
            ));

        when(cartRepository.save(any()))
            .thenReturn(Mono.just(cart));

        StepVerifier.create(cartService.decreaseItem(5L))
            .verifyComplete();
    }

    @Test
    @DisplayName("decreaseItem — удаляет item если count == 1")
    void decreaseItem_delete() {

        CartItem item = new CartItem(1L, 1L, 5L, 1, Instant.now());

        when(cartRepository.findByUserId(10L))
            .thenReturn(Mono.just(cart));

        when(cartItemRepository.findByCartIdAndItemId(1L, 5L))
            .thenReturn(Mono.just(item));

        when(cartItemRepository.delete(item))
            .thenReturn(Mono.empty());

        when(cartItemRepository.findByCartId(1L))
            .thenReturn(Flux.empty());

        when(cartRepository.save(any()))
            .thenReturn(Mono.just(cart));

        StepVerifier.create(cartService.decreaseItem(5L))
            .verifyComplete();
    }

    @Test
    @DisplayName("removeItem — удаляет товар полностью из корзины")
    void removeItem() {

        when(cartRepository.findByUserId(10L)).thenReturn(Mono.just(cart));
        when(cartItemRepository.deleteByCartIdAndItemId(1L, 5L))
            .thenReturn(Mono.empty());

        when(cartItemRepository.findByCartId(1L))
            .thenReturn(Flux.empty());

        when(cartRepository.save(any()))
            .thenReturn(Mono.just(cart));

        StepVerifier.create(cartService.removeItem(5L))
            .verifyComplete();
    }

    @Test
    @DisplayName("clearCart — очищает корзину")
    void clearCart() {

        when(cartItemRepository.deleteAllByCartId(1L))
            .thenReturn(Mono.empty());

        when(cartRepository.findById(1L))
            .thenReturn(Mono.just(cart));

        when(cartRepository.save(any()))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(cartService.clearCart(cart))
            .verifyComplete();

        assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
