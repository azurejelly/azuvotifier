package com.vexsoftware.votifier.util;

import lombok.experimental.UtilityClass;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;

@UtilityClass
public class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a new random token.
     *
     * @return a {@link String} containing a randomly generated token
     */
    public static String newToken() {
        return new BigInteger(130, RANDOM).toString(32);
    }

    /**
     * Converts a token into a {@link Key} using the HMAC-SHA256
     * algorithm.
     *
     * @param token the token as a string
     * @return a {@link Key} generated from the provided token
     */
    public static Key toKey(String token) {
        return new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
