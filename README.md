# Rate Limiter Using Token Bucket Algorithm in Spring Boot 3.5 (Java 21)

This implementation provides a lightweight, configurable, per-endpoint and per-client rate limiter using the Token Bucket algorithm in Spring Boot 3.5 and Java 21. It is fully in-memory, fast, and thread-safe, suitable for single-instance deployments, development environments, or moderate-traffic services.

## Core Concept: Token Bucket Algorithm

The Token Bucket algorithm is used to enforce request throttling while allowing controlled bursts of traffic.

Key elements:

1. Capacity  
   Maximum number of tokens a bucket can hold. Defines the allowable burst size.

2. Refill Rate (tokens per second)  
   Tokens are replenished gradually over time. Defines the sustained throughput limit.

3. Available Tokens  
   Decreases when requests arrive. Increases based on elapsed time multiplied by the refill rate.

4. Decision Logic  
   - If availableTokens ≥ 1 → consume 1 token → allow the request  
   - If availableTokens < 1 → reject the request with HTTP 429 Too Many Requests  

This behavior ensures smooth traffic shaping while still permitting short bursts.

## Summary of Each File and Its Role

### 1. TokenBucket.java  
Implements the actual Token Bucket logic.  
- Stores capacity, available tokens, refill rate, and last refill timestamp.  
- Thread-safe using ReentrantLock.  
- Computes token refill on demand based on elapsed time.  
- Performs request allow/deny decisions via tryConsume().  
This class contains the core rate-limiting algorithm and is used for each client+endpoint combination.

### 2. RateLimiterProperties.java  
Holds per-endpoint rate limit configurations loaded from application.properties.  
- Uses a Map<String, EndpointConfig> to map path patterns like `/hello` or `/api/**` to limits.  
- Each EndpointConfig includes capacity and refillTokensPerSecond.  
- Includes a fallback configuration used when a request path has no matching entry.  
This allows rate limits to be modified without changing code.

### 3. RateLimiterService.java  
Coordinates the rate-limiting decision.  
- Matches incoming request paths to the correct endpoint configuration using AntPathMatcher.  
- Builds a unique key for each client+endpoint (e.g., `127.0.0.1|/hello`).  
- Retrieves or initializes a TokenBucket for that key.  
- Calls tryConsume() to determine allowance.  
This component is the central orchestrator that applies rate limits based on path patterns and client identity.

### 4. RateLimitingInterceptor.java  
Intercepts HTTP requests before they reach controllers.  
- Extracts client IP or X-Forwarded-For header.  
- Invokes RateLimiterService to check if the request should be allowed.  
- If denied, returns HTTP 429 Too Many Requests with a Retry-After header.  
Ensures all API endpoints automatically apply rate limiter rules.

### 5. WebConfig.java  
Registers the RateLimitingInterceptor with Spring MVC.  
- Applies the interceptor to all incoming requests via addInterceptors().  
Without this, the rate limiter would not be executed.

### 6. application.properties  
Defines rate limits per endpoint. Example:

rate-limiter.endpoints[/hello].capacity=5  
rate-limiter.endpoints[/hello].refill-tokens-per-second=1.0  
rate-limiter.endpoints[/api/slow].capacity=5  
rate-limiter.endpoints[/api/slow].refill-tokens-per-second=0.5  
rate-limiter.fallback.capacity=20  
rate-limiter.fallback.refill-tokens-per-second=10.0  

Different endpoints can have different rate limits and fallback settings.

### 7. DemoController.java  
Provides test endpoints such as `/hello`, `/api/slow`, and `/api/fast` to validate the rate limiting behavior.  
Requests to these endpoints will trigger the rate limiter automatically through the interceptor.

## End-to-End Request Flow (In-Memory Operation)

1. A request arrives, for example `GET /api/slow`.  
2. RateLimitingInterceptor extracts client identity (IP or forwarded header) and path.  
3. RateLimiterService determines which endpoint pattern matches (`/api/slow` → configured limits).  
4. Service constructs a bucket key combining client + endpoint pattern (e.g., `127.0.0.1|/api/slow`).  
5. TokenBucket for that key is retrieved or created with configured capacity/refill rate.  
6. TokenBucket performs lazy refill based on elapsed time since last request.  
7. TokenBucket.tryConsume(1) is executed:  
   - If tokens are available → decrement → allow request.  
   - If out of tokens → request is blocked → return 429 Too Many Requests.  
8. Allowed requests reach controller methods; denied ones return error immediately.  
This process occurs on every request and enforces per-client, per-endpoint rate limits efficiently.

## Testing the Rate Limiter (Postman)

- Send rapid consecutive requests to `/hello`.  
- You should receive:  
  - First N requests = 200 OK (N = capacity).  
  - After the bucket is empty = 429 Too Many Requests.  
- Wait for refill (based on configured tokens per second) and request again.  
- Add X-Forwarded-For headers to simulate separate client identities.  
Each client receives its own independent token bucket.

## Running the Application

Run with Gradle:

./gradlew bootRun  

Or package and run:

./gradlew build  
java -jar build/libs/*.jar  

## Summary

This in-memory Token Bucket rate limiter for Spring Boot provides:

- Fast and thread-safe per-endpoint throttling  
- Per-client isolation using IP or X-Forwarded-For  
- Burst tolerance and smooth rate control  
- Simple configuration and maintainability  
- Clear extension path to Redis or API gateway rate limiting  

It is ideal for single-node or moderate-scale systems and forms a strong foundation for more advanced distributed or annotation-based rate-limiting mechanisms.
