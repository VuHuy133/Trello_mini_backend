
package com.trello.config.security;

import com.trello.entity.User;
import com.trello.service.TokenBlacklistService;
import com.trello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;
    @Lazy
    @Autowired
    private UserService userService;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Lazy
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Lazy
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/home", "/login", "/register", "/logout").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                // Public API endpoints (REST API authentication endpoints are public)
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                // Admin endpoints require ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // API endpoints require authentication with JWT exception handling
                .requestMatchers("/api/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // Only return JSON for API requests
                    if (request.getRequestURI().startsWith("/api/")) {
                        unauthorizedHandler.commence(request, response, authException);
                    } else {
                        // For web pages, redirect to login
                        response.sendRedirect("/login");
                    }
                })
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    try {
                        Long userId = Long.parseLong(authentication.getName());
                        User user = userService.getUserById(userId);
                        if (user != null) {
                            request.getSession().setAttribute("user", user);
                            // Set default role if null
                            if (user.getRole() == null) {
                                user.setRole("USER");
                            }
                        } else {
                            // Create a default user object if not found
                            User defaultUser = new User();
                            defaultUser.setRole("USER");
                            request.getSession().setAttribute("user", defaultUser);
                        }
                        
                        // Generate JWT token và lưu vào Redis
                        String token = jwtTokenProvider.generateToken(userId.toString());
                        long ttl = jwtTokenProvider.getTokenExpirationInSeconds(token);
                        tokenBlacklistService.saveActiveToken(token, userId.toString(), ttl);
                        // Lưu token vào session để dùng cho các request tiếp theo
                        request.getSession().setAttribute("jwtToken", token);
                        // Redirect to /admin if user is admin, otherwise to /user
                        String redirectUrl = (user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole())) ? "/admin" : "/user";
                        response.sendRedirect(redirectUrl);
                    } catch (Exception e) {
                        System.out.println("ERROR in login successHandler: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            response.sendRedirect("/user");
                        } catch (Exception ex) {
                            System.out.println("ERROR redirecting after login: " + ex.getMessage());
                        }
                    }
                })
                .failureUrl("/login?error")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("trello-mini-remember-key")
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
                .rememberMeParameter("remember-me")
                .userDetailsService(customUserDetailsService)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler((request, response, authentication) -> {
                    // Lấy JWT token từ session và đưa vào blacklist trước khi invalidate
                    String jwtToken = (String) request.getSession().getAttribute("jwtToken");
                    if (jwtToken != null) {
                        try {
                            long ttl = jwtTokenProvider.getTokenExpirationInSeconds(jwtToken);
                            tokenBlacklistService.blacklistToken(jwtToken, ttl);
                        } catch (Exception e) {
                            // Log nhưng không block logout
                        }
                    }
                })
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oauth2SuccessHandler)
                .permitAll()
            );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}
