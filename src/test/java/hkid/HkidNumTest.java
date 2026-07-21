package hkid;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidNumTest {
    @Test
    void constructorAcceptsSupportedFormats() {
        assertEquals("A100000(7)", new HkidNum("A100000").toString(HkidNum.Format.Complete));
        assertEquals("BC987654(8)", new HkidNum("BC987654").toString(HkidNum.Format.Complete));
        assertEquals("C123456(9)", new HkidNum("C1234569").toString(HkidNum.Format.Complete));
        assertEquals("DE123456(2)", new HkidNum("DE1234562").toString(HkidNum.Format.Complete));
        assertEquals("Z999999(0)", new HkidNum("Z999999(0)").toString(HkidNum.Format.Complete));
        assertEquals("A123456(3)", new HkidNum("A", "123456").toString(HkidNum.Format.Complete));
        assertEquals("A123456(3)", new HkidNum("A", "123456", "3").toString(HkidNum.Format.Complete));
    }

    @Test
    void constructorRejectsInvalidFormats() {
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum(null));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum(""));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("01234567"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("#$123456"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("AB12345(9)"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("AB1234567(0)"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("AB1C345(9)"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("AB1234567((9)"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("AB1234567(9"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("AB12345679)"));
    }

    @Test
    void constructorRejectsConflictingCheckDigitFormats() {
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("A1234563(3)"));
    }

    @Test
    void constructorRejectsInvalidCheckDigits() {
        assertThrows(HkidNum.InvalidCheckDigitException.class, () -> new HkidNum("A123456(7)"));
        assertThrows(HkidNum.InvalidCheckDigitException.class, () -> new HkidNum("AB123456(0)"));
        assertThrows(HkidNum.InvalidCheckDigitException.class, () -> new HkidNum("A", "123456", "7"));
    }

    @Test
    void splitConstructorValidatesEachParameterIndependently() {
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum(null, "A123456"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("A1", "23456"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("A", "1234563"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> new HkidNum("A", "123456", "33"));
    }

    @Test
    void settersRecalculateCheckDigit() {
        HkidNum hkidNum = new HkidNum("A123456(3)");

        hkidNum.setPrefix("B");
        assertEquals("B", hkidNum.getPrefix());
        assertEquals("6", hkidNum.getCheckDigit());

        hkidNum.setNumerals("654321");
        assertEquals("654321", hkidNum.getNumerals());
        assertEquals("4", hkidNum.getCheckDigit());
    }

    @Test
    void settersRejectInvalidValues() {
        HkidNum hkidNum = new HkidNum("A123456(3)");

        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> hkidNum.setPrefix("1"));
        assertThrows(HkidNum.InvalidHkidNumFormatException.class, () -> hkidNum.setNumerals("ABCDE"));
    }

    @Test
    void formatsOutput() {
        HkidNum hkidNum = new HkidNum("A123456(3)");

        assertEquals("A123456", hkidNum.toString());
        assertEquals("A123456", hkidNum.toString(HkidNum.Format.WithoutCheckDigit));
        assertEquals("A1234563", hkidNum.toString(HkidNum.Format.WithoutParentheses));
        assertEquals("A123456(3)", hkidNum.toString(HkidNum.Format.Complete));
    }

    @Test
    void validatesCheckDigit() {
        assertTrue(HkidNum.validateCheckDigit("A123456", "3"));
        assertTrue(HkidNum.validateCheckDigit("a123456", "3"));
        assertFalse(HkidNum.validateCheckDigit("A123456", "7"));
        assertFalse(HkidNum.validateCheckDigit("A12345", "3"));
        assertFalse(HkidNum.validateCheckDigit("A1234563", "3"));
        assertFalse(HkidNum.validateCheckDigit("A123456(3)", "3"));
    }

    @Test
    void exposesDefinedPrefixDescriptionsInBothLanguages() {
        HkidNum hkidNum = new HkidNum("A123456");

        assertEquals(HkidNum.DefinedPrefix.A, hkidNum.getDefinedPrefix().orElse(null));
        assertEquals(HkidNum.DefinedPrefix.A.getDescription(), hkidNum.getPrefixDescription());
        assertEquals(HkidNum.DefinedPrefix.A.getTraditionalChineseDescription(),
                hkidNum.getPrefixTraditionalChineseDescription());
    }

    @Test
    void usesDescriptionFallbacksForValidUndefinedPrefixes() {
        HkidNum hkidNum = new HkidNum("Q123456");

        assertFalse(hkidNum.getDefinedPrefix().isPresent());
        assertEquals("No predefined description is available for prefix Q.", hkidNum.getPrefixDescription());
        assertEquals("字頭 Q 沒有預定義說明。", hkidNum.getPrefixTraditionalChineseDescription());
    }

    @Test
    void everyValidPrefixSupportsDescriptionAccess() {
        for (char first = 'A'; first <= 'Z'; first++) {
            assertDescriptionAccessDoesNotThrow(String.valueOf(first));
            for (char second = 'A'; second <= 'Z'; second++) {
                assertDescriptionAccessDoesNotThrow(new String(new char[]{first, second}));
            }
        }
    }

    private void assertDescriptionAccessDoesNotThrow(String prefix) {
        HkidNum hkidNum = new HkidNum(prefix, "123456");
        assertDoesNotThrow(hkidNum::getPrefixDescription);
        assertDoesNotThrow(hkidNum::getPrefixTraditionalChineseDescription);
    }

    @Test
    void generatesRandomNumber() {
        HkidNum hkidNum = HkidNumUtil.genRandomHkidNum();

        assertNotNull(hkidNum);
        assertDoesNotThrow(() -> new HkidNum(hkidNum.toString(HkidNum.Format.Complete)));

        System.out.println("HKID Number: " + hkidNum);
        System.out.println("HKID Number (Without Parentheses): " + hkidNum.toString(HkidNum.Format.WithoutParentheses));
        System.out.println("HKID Number (Complete): " + hkidNum.toString(HkidNum.Format.Complete));
    }

    @Test
    void generatesAsciiNumeralsRegardlessOfDefaultLocale() {
        Locale originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("hi-IN-u-nu-deva"));

            HkidNum hkidNum = HkidNumUtil.genRandomHkidNum();

            assertTrue(hkidNum.getNumerals().matches("[0-9]{6}"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
        }
    }
}
