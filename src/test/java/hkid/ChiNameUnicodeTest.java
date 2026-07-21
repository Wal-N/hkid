package hkid;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChiNameUnicodeTest {
    private static final String SUPPLEMENTARY_HAN = "\uD840\uDC00"; // U+20000

    @Test
    void acceptsHanCharactersOutsideTheBasicCjkRange() {
        assertTrue(ChiNameUtil.isChinese("\u3400"));
        assertTrue(ChiNameUtil.isChinese("\u9FA6"));
        assertTrue(ChiNameUtil.isChinese(SUPPLEMENTARY_HAN));
        assertTrue(ChiNameUtil.isChinese("\uF900"));
        assertFalse(ChiNameUtil.isChinese("A"));
        assertFalse(ChiNameUtil.isChinese(""));
    }

    @Test
    void countsSupplementaryHanAsOneCharacter() {
        String fiveBmpCharacters = "\u4E00\u4E01\u4E02\u4E03\u4E04";

        assertDoesNotThrow(() -> new ChiName(
                SUPPLEMENTARY_HAN,
                fiveBmpCharacters,
                Arrays.asList("0001", "0002", "0003", "0004", "0005", "0006")));
        assertThrows(IllegalArgumentException.class,
                () -> new ChiName(SUPPLEMENTARY_HAN, fiveBmpCharacters + "\u4E05"));
    }

    @Test
    void seedEntryAcceptsOneSupplementaryHanCodePoint() {
        ChineseNameEntry entry = new ChineseNameEntry("0001", SUPPLEMENTARY_HAN, "Wan", false, 1);

        assertEquals(SUPPLEMENTARY_HAN, entry.getCharacter());
    }
}
