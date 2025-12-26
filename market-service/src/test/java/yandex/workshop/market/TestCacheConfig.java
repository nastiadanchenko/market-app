package yandex.workshop.market;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@EnableCaching
@TestConfiguration
public class TestCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "items",
            "itemList",
            "itemsInCartTotal",
            "itemsInCart",
            "userCart",
            "cartItems",
            "cartTotal"

        );
    }
}
