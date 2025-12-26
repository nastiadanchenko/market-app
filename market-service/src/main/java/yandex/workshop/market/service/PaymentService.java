package yandex.workshop.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yandex.workshop.market.domain.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WebClient paymentWebClient;

    public Mono<Double> getBalance() {
        return paymentWebClient.get()
            .uri("http://localhost:8081/payments/balance")
            .retrieve()
            .bodyToMono(BalanceResponse.class)
            .map(BalanceResponse::getBalance);
    }

    public Mono<ResponseEntity<PaymentResponse>> pay(PaymentRequest req) {
        return paymentWebClient.post()
            .uri("http://localhost:8081/payments/pay")
            .bodyValue(req)
            .retrieve()
            .toEntity(PaymentResponse.class);
    }
}
