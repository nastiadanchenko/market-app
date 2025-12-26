package yandex.workshop.market.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.Users;
import yandex.workshop.market.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Получить текущего авторизованного пользователя.
     * Если пользователя нет в БД — создать.
     */
    public Mono<Users> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(auth -> {
                log.debug("Authentication: {}", auth.getAuthentication().getName());
                log.debug("Principal: {}", auth.getAuthentication().getPrincipal());
                return auth;
            })
            .map(ctx -> (DefaultOidcUser) ctx.getAuthentication().getPrincipal())
            .flatMap(jwt -> {
                UUID keycloakId = UUID.fromString(jwt.getAttribute("sub"));
                String username = jwt.getAttribute("preferred_username");
                String email = jwt.getAttribute("email");
                log.debug("Climes from JWT: name={}, email={}, sub={}",
                    username,
                    email,
                    keycloakId);
                Users user = new Users();
                user.setName(username);
                user.setEmail(email);
                user.setKeycloakId(keycloakId);
                return userRepository.findByKeycloakId(keycloakId)
                    .switchIfEmpty(
                        userRepository.save(user));
            });
    }
}
