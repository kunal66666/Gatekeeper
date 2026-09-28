package in.coderarmy.gatekeeper.gateway.service;

import in.coderarmy.gatekeeper.gateway.dto.RouteDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(RouteDefinition route, String clientIp) {
        if (route.requestsPerMinute() == null || route.requestsPerMinute() <= 0) {
            return true;
        }

        String key = "ratelimit:" + route.routeId() + ":" + clientIp;
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        try {
            // Remove requests older than 60 seconds
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // Count requests in the current 60s sliding window
            Long currentCount = redisTemplate.opsForZSet().zCard(key);

            if (currentCount != null && currentCount >= route.requestsPerMinute()) {
                log.warn("Rate limit exceeded for route {} from IP {}: count={}/min",
                        route.routeId(), clientIp, currentCount);
                return false;
            }

            // Record this request
            redisTemplate.opsForZSet().add(key, String.valueOf(now), (double) now);
            redisTemplate.expire(key, Duration.ofSeconds(120));
            return true;

        } catch (Exception ex) {
            // Fail-open strategy: allow request if Redis is temporarily unreachable
            log.error("Redis rate limiter unavailable, failing open: {}", ex.getMessage());
            return true;
        }
    }
}
