package com.example.ratelimit.config;

import jakarta.websocket.EndpointConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /**
     * Each map key is an Ant-style path pattern (e.g. /api/foo/** or /hello).
     * Value contains capacity / refill rate for that endpoint.
     *
     * Example :
     * rate-limiter:
     *   endpoints:
     *     /api/hello:
     *       capacity: 20
     *       refill-tokens-per-second: 10.0
     */
    private Map<String, EndpointConfig> endpoints = new HashMap<>();

    // If a request's path doesn't match any pattern below, use fallback (optional)
    private EndpointConfig fallback = new EndpointConfig(20, 10.0);

    public Map<String, EndpointConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    public EndpointConfig getFallback() {
        return fallback;
    }

    public void setFallback(EndpointConfig fallback) {
        this.fallback = fallback;
    }

    public static class EndpointConfig {
        private long capacity = 20;
        private double refillTokensPerSecond = 10.0;

        public EndpointConfig() {}

        public EndpointConfig(long capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public double getRefillTokensPerSecond() {
            return refillTokensPerSecond;
        }

        public void setRefillTokensPerSecond(double refillTokensPerSecond) {
            this.refillTokensPerSecond = refillTokensPerSecond;
        }
    }
}
