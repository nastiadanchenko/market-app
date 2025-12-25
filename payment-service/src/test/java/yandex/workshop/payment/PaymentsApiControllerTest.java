package yandex.workshop.payment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import yandex.workshop.payment.model.PaymentRequest;

@WebFluxTest(controllers = PaymentsApiController.class)
@Import(SecurityConfig.class)
public class PaymentsApiControllerTest {

    private WebTestClient authenticatedClient() {
        return webTestClient
            .mutateWith(SecurityMockServerConfigurers.mockJwt());
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /payments/balance — возвращает текущий баланс")
    void shouldReturnBalance() {
        authenticatedClient()
            .get()
            .uri("/payments/balance")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.balance").exists();
    }

    @Test
    @DisplayName("POST /payments/pay — успешный платёж")
    void shouldPaySuccessfully() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(1.0);

        authenticatedClient()
            .post()
            .uri("/payments/pay")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").value(msg ->
                Assertions.assertTrue(msg.toString().contains("Payment successful"))
            );
    }

    @Test
    @DisplayName("POST /payments/pay — недостаточно средств")
    void shouldFailWhenInsufficientFunds() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(1_000_000.0);

        authenticatedClient()
            .post()
            .uri("/payments/pay")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.PAYMENT_REQUIRED)
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("POST /payments/pay — некорректная сумма")
    void shouldFailOnInvalidAmount() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(-10.0);

        authenticatedClient()
            .post()
            .uri("/payments/pay")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").isEqualTo("Invalid payment amount");
    }

}
