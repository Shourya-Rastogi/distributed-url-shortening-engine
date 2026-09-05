package com.distributed.urlshortener.core;

import java.util.regex.Pattern;

/**
 * High-performance Bijective Base62 Encoder / Decoder.
 * Maps 64-bit integer sequence IDs to clean, URL-safe alphanumeric strings.
 * Alphabet: [0-9a-zA-Z] (62 characters)
 */
public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62
    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");

    // Lookup index for O(1) decoding
    private static final int[] CHAR_INDEX = new int[128];

    static {
        for (int i = 0; i < CHAR_INDEX.length; i++) {
            CHAR_INDEX[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length(); i++) {
            CHAR_INDEX[ALPHABET.charAt(i)] = i;
        }
    }

    private Base62Encoder() {
        // Prevent instantiation
    }

    /**
     * Encodes a positive 64-bit integer ID into a Base62 string.
     *
     * @param id non-negative long value
     * @return Base62 encoded string
     */
    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative: " + id);
        }
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder(11); // Long.MAX_VALUE in Base62 is 11 chars
        long current = id;
        while (current > 0) {
            int remainder = (int) (current % BASE);
            sb.append(ALPHABET.charAt(remainder));
            current /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to its 64-bit integer sequence ID.
     *
     * @param str Base62 string
     * @return 64-bit sequence ID
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Base62 string cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= CHAR_INDEX.length || CHAR_INDEX[c] == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + CHAR_INDEX[c];
        }
        return result;
    }

    /**
     * Validates whether a custom alias conforms to safe alphanumeric rules.
     *
     * @param alias the requested custom alias
     * @return true if valid
     */
    public static boolean isValidCustomAlias(String alias) {
        if (alias == null) {
            return false;
        }
        return CUSTOM_ALIAS_PATTERN.matcher(alias).matches();
    }
}
