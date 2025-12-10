package com.example.ratelimit.service;

import com.example.ratelimit.TokenBucket;
import com.example.ratelimit.config.RateLimiterProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimiterService {

    private final RateLimiterProperties properties;

    /**
     * Key format: clientKey + "|" + endpointPattern
     * This keeps per-client-per-endpoint buckets.
     */
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // cached endpoint configs and patterns
    private final ConcurrentMap<String, RateLimiterProperties.EndpointConfig> endpointConfigs = new ConcurrentHashMap<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimiterService(RateLimiterProperties properties) {
        this.properties = properties;

        // copy initial endpoints to local cache for faster access
        this.endpointConfigs.putAll(properties.getEndpoints());
    }

    /**
     * Allow request for a client + requestPath. Returns true if allowed.
     */
    public boolean allowRequest(String clientKey, String requestPath) {
        // find matching pattern
        String matchedPattern = findBestMatch(requestPath);

        RateLimiterProperties.EndpointConfig config;
        if (matchedPattern == null) {
            config = properties.getFallback();
            matchedPattern = "__FALLBACK__";
        } else {
            config = endpointConfigs.getOrDefault(matchedPattern, properties.getFallback());
        }

        // compose bucket key
        String bucketKey = clientKey + "|" + matchedPattern;

        // create-if-absent a token bucket for this client+endpoint
        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k ->
                new TokenBucket(config.getCapacity(), config.getRefillTokensPerSecond())
        );

        return bucket.tryConsume(1);
    }

    /**
     * Find the best matching endpoint pattern for the requestPath.
     * We pick the most specific match (longest pattern).
     */
    private String findBestMatch(String requestPath) {
        if (endpointConfigs.isEmpty()) {
            return null;
        }

        return endpointConfigs.keySet().stream()
                .filter(pattern -> pathMatcher.match(pattern, requestPath))
                .max(Comparator.comparingInt(String::length)) // most specific (longest pattern)
                .orElse(null);
    }

    /**
     * Optional: reload configs at runtime (if you want to wire a /actuator endpoint to change limits).
     */
    public void reloadEndpointConfigs(Map<String, RateLimiterProperties.EndpointConfig> updated) {
        endpointConfigs.clear();
        endpointConfigs.putAll(updated);
    }


}
