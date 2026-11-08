package de.werkmundo.offer.application.security;

public final class GeneratedOfferAccessToken {
    private final String token;
    private final String tokenHash;

    GeneratedOfferAccessToken(String token, String tokenHash) {
        this.token = token;
        this.tokenHash = tokenHash;
    }

    public String token() {
        return token;
    }

    public String tokenHash() {
        return tokenHash;
    }

    @Override
    public String toString() {
        return "GeneratedOfferAccessToken[redacted]";
    }
}
