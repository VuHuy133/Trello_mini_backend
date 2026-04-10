package com.trello.config.security;

import com.trello.entity.User;
import com.trello.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2 Authentication Success Handler
 * 
 * Handles Google OAuth2 login:
 * 1. Extract user info from OAuth2 token
 * 2. Create/update user in DB
 * 3. Generate JWT token
 * 4. Redirect to home with JWT
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            // Get OAuth2 authentication
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            
            // Extract user info from OAuth2
            Object principal = oauth2Token.getPrincipal();
            String email = null;
            String name = null;
            String picture = null;

            if (principal instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principal;
                email = oidcUser.getEmail();
                name = oidcUser.getFullName();
                picture = oidcUser.getPicture();
            }

            if (email == null) {
                log.warn("OAuth2 login failed: email not found in token");
                getRedirectStrategy().sendRedirect(request, response, "/login?error=no_email");
                return;
            }

            log.info("OAuth2 login for email: {}", email);

            // Find or create user in DB
            final String finalName = name != null ? name : email.split("@")[0];
            User user = userService.saveOAuth2User(email, finalName);
            log.info("OAuth2 user ready: id={}, email={}", user.getId(), email);

            // Generate JWT token
            String jwtToken = jwtTokenProvider.generateToken(user.getId().toString());
            long tokenTtl = jwtTokenProvider.getTokenExpirationInSeconds(jwtToken);

            // Save to session
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute("jwtToken", jwtToken);

            log.info("JWT token generated for user: {}", user.getId());

            // Redirect to user page after successful OAuth2 login
            getRedirectStrategy().sendRedirect(request, response, "/user");

        } catch (Exception e) {
            log.error("Error during OAuth2 authentication success: {}", e.getMessage(), e);
            e.printStackTrace();
            getRedirectStrategy().sendRedirect(request, response, "/login?error=authentication_failed");
        }
    }
}
