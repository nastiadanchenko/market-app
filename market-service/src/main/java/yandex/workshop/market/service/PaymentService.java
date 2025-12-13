package yandex.workshop.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yandex.workshop.market.api.DefaultApi;
import yandex.workshop.market.domain.*;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final DefaultApi paymentApi;

    public Mono<Double> getBalance() {
        return paymentApi.paymentsBalanceGetWithHttpInfo()
            .mapNotNull(HttpEntity::getBody)
            .map(BalanceResponse::getBalance);
    }

    public Mono<ResponseEntity<PaymentResponse>> pay(PaymentRequest req) {
        if (req == null) {
            return Mono.error(new IllegalArgumentException("PaymentRequest must not be null"));
        }

        return paymentApi.paymentsPayPostWithHttpInfo(req);
    }
}
