package com.distributed.urlshortener.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    @DisplayName("Encode and decode boundary and typical IDs should preserve bijection")
    void testBijectiveEncodeDecode() {
        long[] testIds = {0L, 1L, 61L, 62L, 125L, 3844L, 238328L, 14776336L, 916132832L, 100000000000L, Long.MAX_VALUE / 4};

        for (long id : testIds) {
            String encoded = Base62Encoder.encode(id);
            assertNotNull(encoded);
            assertFalse(encoded.isEmpty());

            long decoded = Base62Encoder.decode(encoded);
            assertEquals(id, decoded, "Failed bijective roundtrip for ID: " + id + " (encoded: " + encoded + ")");
        }
    }

    @Test
    @DisplayName("Negative ID encoding should throw IllegalArgumentException")
    void testNegativeIdEncoding() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
    }

    @Test
    @DisplayName("Invalid characters during decoding should throw IllegalArgumentException")
    void testInvalidCharacterDecode() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("abc!123"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("hello@world"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"summer2026", "deal_of_the_day", "promo-50", "blackfriday", "abc-123_xyz"})
    @DisplayName("Valid custom aliases should pass validation")
    void testValidCustomAliases(String alias) {
        assertTrue(Base62Encoder.isValidCustomAlias(alias));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "invalid alias with space", "bad@char!", "toolongtoolongtoolongtoolongtoolongtoolong"})
    @DisplayName("Invalid custom aliases should fail validation")
    void testInvalidCustomAliases(String alias) {
        assertFalse(Base62Encoder.isValidCustomAlias(alias));
    }
}
