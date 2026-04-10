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

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(user.getId().toString());
            long tokenTtl = jwtTokenProvider.getTokenExpirationInSeconds(token);
            System.out.println("DEBUG: Generated token TTL: " + tokenTtl + " seconds");

            // Lưu token vào Redis
            tokenBlacklistService.saveActiveToken(
                    token, user.getId().toString(),
                    tokenTtl);

            UserResponse userResponse = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .build();

            // Response with token in header and user data in body
            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .message("Đăng nhập thành công")
                    .statusCode(HttpStatus.OK.value())
                    .data(userResponse)
                    .build();

            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + token)
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
     * Client should invalidate token on their side
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        // Validate token exists
        if (token == null || !token.startsWith("Bearer ")) {
            ApiResponse<?> response = ApiResponse.builder()
                    .success(false)
                    .message("Token không hợp lệ hoặc không được cung cấp")
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            String jwtToken = token.substring(7);
            
            // Validate token
            if (!jwtTokenProvider.validateToken(jwtToken)) {
                ApiResponse<?> response = ApiResponse.builder()
                        .success(false)
                        .message("Token hết hạn hoặc không hợp lệ")
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .build();
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Đưa token vào blacklist Redis
            tokenBlacklistService.blacklistToken(
                    jwtToken,
                    jwtTokenProvider.getTokenExpirationInSeconds(jwtToken));

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
}
