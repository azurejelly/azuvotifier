package com.vexsoftware.votifier.bukkit.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class TokenUtil {

    private TokenUtil() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String newToken() {
        return new BigInteger(130, RANDOM).toString(32);
    }
}
