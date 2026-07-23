package io.github.wal_n.hkid.name;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChineseNameUnicodeTest {
    private static final String SUPPLEMENTARY_IDEOGRAPH = "\uD840\uDC00"; // U+20000 𠀀
    private static final String HKSCS_2016_IDEOGRAPH = "\uD874\uDE5D"; // U+2D25D 𭉝

    @Test
    void acceptsUnifiedIdeographsOutsideTheBasicCjkRange() {
        assertTrue(ChineseNameUtil.isChinese("\u3400")); // U+3400 㐀, Extension A
        assertTrue(ChineseNameUtil.isChinese("\u9FA6")); // U+9FA6 龦, main CJK block
        assertTrue(ChineseNameUtil.isChinese(SUPPLEMENTARY_IDEOGRAPH));
        assertTrue(ChineseNameUtil.isChinese("\uFA0E")); // U+FA0E 﨎, included by Unified_Ideograph
        assertFalse(ChineseNameUtil.isChinese("\uF900")); // U+F900 豈, compatibility form
        assertFalse(ChineseNameUtil.isChinese(
                new String(Character.toChars(0x2F800)))); // U+2F800 丽, compatibility form
        assertFalse(ChineseNameUtil.isChinese("A"));
        assertFalse(ChineseNameUtil.isChinese(""));
    }

    @Test
    void usesFixedCjkIdeographRepertoireInsteadOfJdkHanScriptData() {
        assertTrue(ChineseNameUtil.isChinese(HKSCS_2016_IDEOGRAPH));
        assertFalse(ChineseNameUtil.isChinese("\u2F00")); // U+2F00 ⼀, Kangxi radical symbol
        assertFalse(ChineseNameUtil.isChinese("\u2E80")); // U+2E80 ⺀, CJK radical symbol
        assertFalse(ChineseNameUtil.isChinese(
                new String(Character.toChars(0x323B0)))); // U+323B0 𲎰, added after Unicode 15.1
    }

    @Test
    void countsSupplementaryIdeographAsOneCharacter() {
        String fiveBmpCharacters = "\u4E00\u4E01\u4E02\u4E03\u4E04";

        assertDoesNotThrow(() -> new ChineseName(
                SUPPLEMENTARY_IDEOGRAPH,
                fiveBmpCharacters,
                Arrays.asList("0001", "0002", "0003", "0004", "0005", "0006")));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName(SUPPLEMENTARY_IDEOGRAPH, fiveBmpCharacters + "\u4E05"));
    }

    @Test
    void seedEntryAcceptsOneSupplementaryIdeographCodePoint() {
        ChineseNameEntry entry = new ChineseNameEntry("0001", SUPPLEMENTARY_IDEOGRAPH, "Wan", false, 1);

        assertEquals(SUPPLEMENTARY_IDEOGRAPH, entry.getCharacter());
    }
}
