package com.vexsoftware.votifier.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Provides utilities for working with Minecraft usernames.
 *
 * @author azurejelly
 * @since 3.4.0
 */
@UtilityClass
public class UsernameUtil {

    /**
     * A regular expression for Minecraft usernames.
     *
     * <p>Usernames must be between 2 and 16 characters and may only contain
     * letters (A-Z, a-z), digits (0-9), and underscores ({@code _}).
     */
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    /**
     * Checks whether a Minecraft username is valid by comparing it against the
     * {@link #USERNAME_PATTERN} regular expression.
     *
     * @param username The username to check
     * @return {@code true} if valid, {@code false} otherwise.
     * @throws IllegalArgumentException if the provided username is {@code null}
     */
    public static boolean isValid(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Must provide a string");
        }

        return USERNAME_PATTERN
                .matcher(username)
                .matches();
    }
}
