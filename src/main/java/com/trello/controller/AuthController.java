package com.trello.controller;

import com.trello.dto.ApiResponse;
import com.trello.dto.LoginRequest;
import com.trello.dto.UserResponse;
import com.trello.entity.User;
import com.trello.exception.UserAlreadyExistsException;
import com.trello.config.security.JwtTokenProvider;
import com.trello.service.TokenBlacklistService;
import com.trello.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * User registration endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody User newUser) {
        try {
            // Check if email already exists
            if (userService.isEmailExist(newUser.getEmail())) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Email đã tồn tại, vui lòng sử dụng email khác")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Register user with USER role
            User registeredUser = userService.handleRegister(newUser);

            UserResponse userResponse = UserResponse.builder()
                    .id(registeredUser.getId())
                    .username(registeredUser.getUsername())
                    .email(registeredUser.getEmail())
                    .role(registeredUser.getRole())
                    .createdAt(registeredUser.getCreatedAt())
                    .build();

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Đăng ký thành công")
                    .statusCode(HttpStatus.CREATED.value())
                    .data(userResponse)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (UserAlreadyExistsException e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Find user by email
            Optional<User> userOpt = userService.getUserByEmail(request.getEmail());

            if (!userOpt.isPresent()) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Email hoặc mật khẩu không chính xác")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            User user = userOpt.get();

            // Validate password
            if (!userService.validatePassword(request.getPassword(), user.getPassword())) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Email hoặc mật khẩu không chính xác")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Generate Access Token (24h)
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId().toString());
            long accessTokenTtl = jwtTokenProvider.getTokenExpirationInSeconds(accessToken);
            System.out.println("DEBUG: Generated access token TTL: " + accessTokenTtl + " seconds");

            // Save access token vào Redis (active)
            tokenBlacklistService.saveActiveToken(
                    accessToken, user.getId().toString(), accessTokenTtl);

            // Generate Refresh Token (30 days)
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());
            long refreshTokenTtl = jwtTokenProvider.getTokenExpirationInSeconds(refreshToken);
            System.out.println("DEBUG: Generated refresh token TTL: " + refreshTokenTtl + " seconds");

            // Save refresh token vào Redis (refresh)
            tokenBlacklistService.saveRefreshToken(
                    refreshToken, user.getId().toString(), refreshTokenTtl);

            UserResponse userResponse = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .build();

            // Response with tokens in body (stateless - React đọc từ body và lưu vào localStorage)
            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Đăng nhập thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(java.util.Map.of(
                        "user", userResponse,
                        "accessToken", accessToken,
                        "refreshToken", refreshToken,
                        "expiresIn", accessTokenTtl
                    ))
                    .build();

            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Refresh-Token", refreshToken)
                    .body(response);

        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi đăng nhập: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Logout endpoint (REST API)
     * Blacklist cả access token và refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        // Validate access token
        if (token == null || !token.startsWith("Bearer ")) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Access token không hợp lệ hoặc không được cung cấp")
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            String accessToken = token.substring(7);
            
            // Validate access token
            if (!jwtTokenProvider.validateToken(accessToken)) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Access token hết hạn hoặc không hợp lệ")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Đưa access token vào blacklist Redis
            tokenBlacklistService.blacklistToken(
                    accessToken,
                    jwtTokenProvider.getTokenExpirationInSeconds(accessToken));

            // Đưa refresh token vào blacklist (nếu được cung cấp)
            if (refreshToken != null && !refreshToken.isEmpty()) {
                if (jwtTokenProvider.validateToken(refreshToken)) {
                    tokenBlacklistService.blacklistRefreshToken(
                            refreshToken,
                            jwtTokenProvider.getTokenExpirationInSeconds(refreshToken));
                }
            }

            // Logout successful
            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Đăng xuất thành công")
                    .statusCode(HttpStatus.OK.value())
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi đăng xuất: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Refresh access token endpoint
     * Client gửi refresh token để lấy access token mới
     * Refresh token được giữ lại (reuse pattern)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refreshAccessToken(
            @RequestParam(value = "refreshToken") String refreshToken) {
        
        // Validate refresh token
        if (refreshToken == null || refreshToken.isEmpty()) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Refresh token không được cung cấp")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Validate refresh token format
            if (refreshToken.startsWith("Bearer ")) {
                refreshToken = refreshToken.substring(7);
            }

            // Check in blacklist
            if (tokenBlacklistService.isRefreshTokenBlacklisted(refreshToken)) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Refresh token đã bị logout, vui lòng login lại")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Refresh token hết hạn hoặc không hợp lệ")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Extract user ID from refresh token
            String userId = jwtTokenProvider.getUsernameFromJWT(refreshToken);
            
            // Verify refresh token in Redis
            String storedRefreshToken = tokenBlacklistService.getRefreshToken(userId);
            if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Refresh token không khớp với token lưu trong Redis")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Generate new access token (keep refresh token)
            String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
            long newAccessTokenTtl = jwtTokenProvider.getTokenExpirationInSeconds(newAccessToken);

            // Save new access token to Redis active
            tokenBlacklistService.saveActiveToken(newAccessToken, userId, newAccessTokenTtl);
            
            // Do NOT generate new refresh token (reuse pattern)
            // Just return the new access token with old refresh token

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Refresh token thành công, access token mới được cấp")
                    .statusCode(HttpStatus.OK.value())
                    .data(java.util.Map.of(
                        "accessToken", newAccessToken,
                        "refreshToken", refreshToken,  // Return original refresh token
                        "expiresIn", newAccessTokenTtl
                    ))
                    .build();

            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + newAccessToken)
                    .header("X-Refresh-Token", refreshToken)
                    .body(response);

        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Lỗi khi refresh token: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
