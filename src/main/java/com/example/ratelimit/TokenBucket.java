package com.example.ratelimit;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {

    private final Long capacity;
    private final double refillTokensPerSecond;
    private double availableTokens;
    private Instant lastRefillTimestamp;

    private final ReentrantLock lock = new ReentrantLock();


    public TokenBucket(Long capacity, double refillTokensPerSecond) {
        if (capacity <= 0 || refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }
        this.capacity = capacity;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.availableTokens = capacity;
        this.lastRefillTimestamp = Instant.now();
    }

    /**
     * Try to consume the given number of tokens; returns true if successful.
     */
    public boolean tryConsume(long tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens to consume must be positive");
        }

        lock.lock();
        try {
            refill();

            if (availableTokens >= tokens) {
                availableTokens -= tokens;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void refill() {
        Instant now = Instant.now();
        double secondsSinceLastRefill = (now.toEpochMilli() - lastRefillTimestamp.toEpochMilli()) / 1000.0;

        if (secondsSinceLastRefill <= 0) {
            return;
        }

        double tokensToAdd = secondsSinceLastRefill * refillTokensPerSecond;
        availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
        lastRefillTimestamp = now;
    }

    // for monitoring/debugging
    public long getCapacity() {
        return capacity;
    }

    public double getRefillTokensPerSecond() {
        return refillTokensPerSecond;
    }

    public double getAvailableTokens() {
        lock.lock();
        try {
            refill(); // update to accurate view
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }
}
