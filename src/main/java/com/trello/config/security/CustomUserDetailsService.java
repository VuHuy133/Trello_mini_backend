package com.trello.config.security;

import com.trello.entity.User;
import com.trello.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Thử tìm theo email trước (login thường)
        User user = userRepository.findByEmail(identifier).orElse(null);

        // Nếu không tìm thấy theo email, thử tìm theo ID (remember-me auto-login)
        if (user == null) {
            try {
                Long userId = Long.parseLong(identifier);
                user = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException ignored) {
            }
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + identifier);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
