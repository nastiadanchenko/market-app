package yandex.workshop.market.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yandex.workshop.market.entity.Users;
import yandex.workshop.market.repository.RoleRepository;
import yandex.workshop.market.repository.UserRepository;
import yandex.workshop.market.repository.UserRoleRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByName(username)
            .switchIfEmpty(Mono.error(new UsernameNotFoundException("Users not found")))
            .flatMap(this::buildUserDetails);
    }

    private Mono<UserDetails> buildUserDetails(Users user) {
        return userRoleRepository.findByUserId(user.getId())
            .flatMap(userRole -> roleRepository.findById(userRole.getRoleId()))
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collectList()
            .map(authorities ->
                User.withUsername(user.getName())
                    .password(user.getPassword())
                    .authorities(authorities)
                    .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                    .build()
            );
    }
}
