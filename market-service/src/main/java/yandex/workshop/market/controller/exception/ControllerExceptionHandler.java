package yandex.workshop.market.controller.exception;

import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public Mono<String> handleNotFound(NoSuchElementException ex, Model model) {
        log.warn("Entity not found: {}", ex.getMessage());
        return buildErrorPage(model, 404, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<String> handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        return buildErrorPage(model, 500, "Internal Server Error", "Произошла непредвиденная ошибка");
    }

    private Mono<String> buildErrorPage(Model model, int status, String error, String message) {
        model.addAttribute("status", status);
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        return Mono.just("error"); // шаблон templates/error.html
    }
}
