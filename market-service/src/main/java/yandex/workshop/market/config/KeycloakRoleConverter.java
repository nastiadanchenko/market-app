package yandex.workshop.market.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<SimpleGrantedAuthority>> {

    @Override
    public Collection<SimpleGrantedAuthority> convert(Jwt jwt) {

        Map<String, Object> realmAccess =
            jwt.getClaim("realm_access");

        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<String> roles =
            (List<String>) realmAccess.get("roles");

        return roles.stream()
            .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
            .toList();
    }
}
