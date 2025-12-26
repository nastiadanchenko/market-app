package yandex.workshop.market.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import reactor.core.publisher.Flux;
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http
        ,ReactiveJwtAuthenticationConverter jwtAuthenticationConverter
    ) {

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            .authorizeExchange(exchanges -> exchanges
                // публичные страницы
                .pathMatchers(
                    "/",
                    "/items",
                    "/items/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/login"
                ).permitAll()

                // корзина и заказы
                .pathMatchers("/cart/**", "/orders/**").authenticated()

                // любые действия (POST/PUT/DELETE)
                .pathMatchers(HttpMethod.POST, "/**").authenticated()
                .pathMatchers(HttpMethod.PUT, "/**").authenticated()
                .pathMatchers(HttpMethod.DELETE, "/**").authenticated()

                .anyExchange().authenticated()
            )
            .oauth2Login(Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .logout(Customizer.withDefaults());

        return http.build();
    }


    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter(
        KeycloakRoleConverter roleConverter
    ) {
        ReactiveJwtAuthenticationConverter converter =
            new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            jwt -> Flux.fromIterable(roleConverter.convert(jwt))
        );

        return converter;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}
