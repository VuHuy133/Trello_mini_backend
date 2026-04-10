package com.trello.service;

import com.trello.config.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String ACTIVE_PREFIX    = "auth:active:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    /** Lưu token vào Redis sau khi login thành công.
     *  Key: auth:active:{userId}  →  Value: token */
    public void saveActiveToken(String token, String userId, long ttlSeconds) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            log.info("saveActiveToken called - userId: {}, ttlSeconds: {}", userId, ttlSeconds);

            if (ttlSeconds <= 0) {
                ttlSeconds = 86400;
                log.warn("TTL invalid, using default 86400s");
            }

            // Key theo userId để dễ tra cứu và đảm bảo single session
            String key = ACTIVE_PREFIX + userId;
            stringRedisTemplate.opsForValue()
                    .set(key, token, ttlSeconds, TimeUnit.SECONDS);

            log.info("Token saved successfully - key: {}, ttl: {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error saving active token: {}", e.getMessage(), e);
        }
    }

    /** Đưa token vào blacklist khi logout, xoá active key theo userId. */
    public void blacklistToken(String token, long ttlSeconds) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            log.info("blacklistToken called - ttlSeconds: {}", ttlSeconds);

            if (ttlSeconds <= 0) {
                ttlSeconds = 86400;
                log.warn("TTL is invalid (<=0), set default to 86400s");
            }

            // Lưu vào blacklist theo token
            String blacklistKey = BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue()
                    .set(blacklistKey, "1", ttlSeconds, TimeUnit.SECONDS);
            log.info("Token blacklisted successfully - key: {}, ttl: {}s", blacklistKey, ttlSeconds);

            // Xoá active key theo userId (lấy userId từ JWT)
            try {
                String userId = jwtTokenProvider.getUsernameFromJWT(token);
                String activeKey = ACTIVE_PREFIX + userId;
                Boolean deleted = stringRedisTemplate.delete(activeKey);
                log.info("Deleted active key: {}, existed: {}", activeKey, deleted);
            } catch (Exception ex) {
                log.warn("Could not extract userId from token to delete active key: {}", ex.getMessage());
            }

            Boolean exists = stringRedisTemplate.hasKey(blacklistKey);
            log.info("Blacklist key exists after save: {}", exists);

        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage(), e);
        }
    }

    /** Kiểm tra token có trong blacklist không. */
    public boolean isBlacklisted(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            String blacklistKey = BLACKLIST_PREFIX + token;
            boolean result = Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey));
            log.debug("isBlacklisted check - result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error checking blacklist: {}", e.getMessage(), e);
            return false;
        }
    }

    /** Lấy token active của một user (nếu có). */
    public String getActiveToken(String userId) {
        try {
            return stringRedisTemplate.opsForValue().get(ACTIVE_PREFIX + userId);
        } catch (Exception e) {
            log.error("Error getting active token for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
