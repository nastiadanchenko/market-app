package yandex.workshop.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import yandex.workshop.market.api.DefaultApi;

@SpringBootApplication
public class MarketAppApplication {

    @Bean
    public DefaultApi getDefaultApi(){
        return new DefaultApi();
    }
    public static void main(String[] args) {
        SpringApplication.run(MarketAppApplication.class, args);
    }

}
