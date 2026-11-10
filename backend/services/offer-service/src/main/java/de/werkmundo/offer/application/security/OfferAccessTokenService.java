package de.werkmundo.offer.application.security;

import de.werkmundo.offer.domain.Offer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public class OfferAccessTokenService {
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedOfferAccessToken generate() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return new GeneratedOfferAccessToken(token, hash(token));
    }

    public void requireValid(Offer offer, String token, Instant now) {
        Objects.requireNonNull(offer, "offer must not be null");
        if (token == null
                || !offer.hasActiveCustomerAccessTokenAt(now)
                || !matches(token, offer.customerAccessTokenHash())) {
            throw new SecurityException("Invalid or expired offer access token");
        }
    }

    private boolean matches(String token, String expectedHash) {
        if (expectedHash == null) {
            return false;
        }
        byte[] actualHash = hash(token).getBytes(StandardCharsets.US_ASCII);
        byte[] storedHash = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(storedHash, actualHash);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
