package yandex.workshop.payment;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import yandex.workshop.payment.api.DefaultApi;
import yandex.workshop.payment.model.BalanceResponse;
import yandex.workshop.payment.model.PaymentRequest;
import yandex.workshop.payment.model.PaymentResponse;

/**
 * ВНИМАНИЕ:
 * Баланс хранится в памяти приложения.
 * При рестарте контейнера состояние теряется.
 * Реализация предназначена только для учебного / демонстрационного проекта.
 */
@RestController
public class PaymentsApiController implements DefaultApi {

    @Value("${balance.value}")
    private Double balance;

    // Баланс хранится в памяти, сбрасывается при рестарте.
    private final AtomicReference<Double> balanceInCents = new AtomicReference<>(balance);

    @Override
    public Mono<ResponseEntity<BalanceResponse>> paymentsBalanceGet(ServerWebExchange exchange) {
        BalanceResponse r = new BalanceResponse().balance(balance);
        return Mono.just(ResponseEntity.ok(r));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> paymentsPayPost(Mono<PaymentRequest> paymentRequest,
                                                                 ServerWebExchange exchange) {
        return paymentRequest
            .flatMap(req -> {
                Double amount = null;
                amount = req.getAmount();

                if (amount == null || amount < 0.0) {
                    PaymentResponse resp = new PaymentResponse();
                    resp.setSuccess(false);
                    resp.setMessage("Invalid payment amount");
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp));
                }

                Double current = balanceInCents.get();
                if (current < amount) {
                    PaymentResponse resp = new PaymentResponse();
                    resp.setSuccess(false);
                    resp.setMessage("Insufficient funds");
                    return Mono.just(ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(resp));
                }
                balanceInCents.updateAndGet(b -> b - req.getAmount());
                PaymentResponse resp = new PaymentResponse();
                resp.setSuccess(true);
                resp.setMessage("Payment successful. Remaining balance: " + String.format("%.2f", balanceInCents.get()));
                return Mono.just(ResponseEntity.ok(resp));
            })
            .switchIfEmpty(Mono.fromSupplier(() -> {
                PaymentResponse resp = new PaymentResponse();
                resp.setSuccess(false);
                resp.setMessage("Empty request body");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }));
    }
}
