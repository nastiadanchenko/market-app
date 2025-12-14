package yandex.workshop.market;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestCacheConfig.class)
@SpringBootTest(properties = "spring.cache.type=none")
@ActiveProfiles("test")
class MarketAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
