package yandex.workshop.market.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.Users;
import yandex.workshop.market.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Mono<Users> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> {
                log.debug("Current user: {}", ctx.getAuthentication().getName());
                return ctx.getAuthentication().getName();
            })
            .flatMap(userRepository::findByKeycloakId)
            .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")));
    }
}
