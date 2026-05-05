package org.anta.service;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.security.SecureRandom;
import java.time.Duration;

@ApplicationScoped
public class OtpRedisService {

    @ConfigProperty(name = "redis.host", defaultValue = "localhost")
    String redisHost;

    @ConfigProperty(name = "redis.port", defaultValue = "6379")
    int redisPort;

    @ConfigProperty(name = "redis.password", defaultValue = "")
    String redisPassword;

    private JedisPool jedisPool;

    private final SecureRandom rnd = new SecureRandom();

    private static final Duration OTP_TTL = Duration.ofMinutes(2);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    @PostConstruct
    void init() {
        this.jedisPool = new JedisPool(redisHost, redisPort);
    }

    @PreDestroy
    void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    private Jedis jedis() {
        Jedis jedis = jedisPool.getResource();

        if (redisPassword != null
                && !redisPassword.isBlank()
                && !"no-password".equals(redisPassword)) {
            jedis.auth(redisPassword);
        }

        return jedis;
    }

    private String norm(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String keyOtp(String email) {
        return "otp:code:" + email;
    }

    private String keyCooldown(String email) {
        return "otp:cooldown:" + email;
    }

    private String keyAttempts(String email) {
        return "otp:attempts:" + email;
    }

    public String generateAndSave(String rawEmail) {
        String email = norm(rawEmail);

        try (Jedis redis = jedis()) {
            long ttl = redis.ttl(keyCooldown(email));

            if (ttl > 0) {
                throw new IllegalStateException("You can only request OTP again after " + ttl + " seconds");
            }

            String code = String.format("%06d", rnd.nextInt(1_000_000));

            redis.setex(keyOtp(email), OTP_TTL.toSeconds(), code);
            redis.del(keyAttempts(email));
            redis.setex(keyCooldown(email), COOLDOWN.toSeconds(), "1");

            return code;
        }
    }

    public boolean verify(String rawEmail, String otp) {
        String email = norm(rawEmail);

        try (Jedis redis = jedis()) {
            String saved = redis.get(keyOtp(email));

            if (saved == null) {
                return false;
            }

            if (!saved.equals(otp)) {
                String aKey = keyAttempts(email);

                long attempts = redis.incr(aKey);
                long otpTtl = redis.ttl(keyOtp(email));

                if (otpTtl > 0) {
                    redis.expire(aKey, otpTtl);
                }

                if (attempts >= MAX_ATTEMPTS) {
                    redis.del(keyOtp(email));
                    redis.del(aKey);
                }

                return false;
            }

            redis.del(keyOtp(email));
            redis.del(keyAttempts(email));

            return true;
        }
    }
}