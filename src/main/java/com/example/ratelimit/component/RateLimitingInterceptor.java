package com.example.ratelimit.component;

import com.example.ratelimit.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitingInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String clientKey = resolveClientKey(request);
        String requestPath = request.getRequestURI();

        boolean allowed = rateLimiterService.allowRequest(clientKey, requestPath);

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            // helpful headers for clients
            response.setHeader("Retry-After", "1"); // best-effort, you can compute precise wait
            response.getWriter().write("Too Many Requests - Rate limit exceeded for endpoint: " + requestPath);
            return false;
        }

        return true;
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Simplest: client IP. Prefer API key / auth subject in production.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        System.out.println("X-Forwarded-For===>"+forwardedFor);
        System.out.println("request.getRemoteAddr===>"+request.getRemoteAddr());
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
