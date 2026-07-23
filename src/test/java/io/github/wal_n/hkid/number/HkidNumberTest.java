package io.github.wal_n.hkid.number;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidNumberTest {
    @Test
    void constructorAcceptsSupportedFormats() {
        assertEquals("A100000(7)", new HkidNumber("A100000").toString(HkidNumber.Format.Complete));
        assertEquals("BC987654(8)", new HkidNumber("BC987654").toString(HkidNumber.Format.Complete));
        assertEquals("C123456(9)", new HkidNumber("C1234569").toString(HkidNumber.Format.Complete));
        assertEquals("DE123456(2)", new HkidNumber("DE1234562").toString(HkidNumber.Format.Complete));
        assertEquals("Z999999(0)", new HkidNumber("Z999999(0)").toString(HkidNumber.Format.Complete));
        assertEquals("A123456(3)", new HkidNumber("A", "123456").toString(HkidNumber.Format.Complete));
        assertEquals("A123456(3)", new HkidNumber("A", "123456", "3").toString(HkidNumber.Format.Complete));
    }

    @Test
    void constructorRejectsInvalidFormats() {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber(null));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber(""));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("01234567"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("#$123456"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("AB12345(9)"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("AB1234567(0)"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("AB1C345(9)"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("AB1234567((9)"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("AB1234567(9"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("AB12345679)"));
    }

    @Test
    void constructorRejectsConflictingCheckDigitFormats() {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A1234563(3)"));
    }

    @Test
    void constructorRejectsInvalidCheckDigits() {
        assertThrows(HkidNumber.InvalidCheckDigitException.class, () -> new HkidNumber("A123456(7)"));
        assertThrows(HkidNumber.InvalidCheckDigitException.class, () -> new HkidNumber("AB123456(0)"));
        assertThrows(HkidNumber.InvalidCheckDigitException.class, () -> new HkidNumber("A", "123456", "7"));
    }

    @Test
    void splitConstructorValidatesEachParameterIndependently() {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber(null, "A123456"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A1", "23456"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A", "1234563"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A", "123456", "33"));
    }

    @Test
    void settersRecalculateCheckDigit() {
        HkidNumber hkidNumber = new HkidNumber("A123456(3)");

        hkidNumber.setPrefix("B");
        assertEquals("B", hkidNumber.getPrefix());
        assertEquals("6", hkidNumber.getCheckDigit());

        hkidNumber.setNumerals("654321");
        assertEquals("654321", hkidNumber.getNumerals());
        assertEquals("4", hkidNumber.getCheckDigit());
    }

    @Test
    void settersRejectInvalidValues() {
        HkidNumber hkidNumber = new HkidNumber("A123456(3)");

        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> hkidNumber.setPrefix("1"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> hkidNumber.setNumerals("ABCDE"));
    }

    @Test
    void formatsOutput() {
        HkidNumber hkidNumber = new HkidNumber("A123456(3)");

        assertEquals("A123456", hkidNumber.toString());
        assertEquals("A123456", hkidNumber.toString(HkidNumber.Format.WITHOUT_CHECK_DIGIT));
        assertEquals("A1234563", hkidNumber.toString(HkidNumber.Format.WithoutParentheses));
        assertEquals("A123456(3)", hkidNumber.toString(HkidNumber.Format.Complete));
    }

    @Test
    void masksAllButLastThreeNumerals() {
        HkidNumber hkidNumber = new HkidNumber("A123456(3)");

        assertEquals("****456(*)", hkidNumber.toMaskedString());
        assertEquals("****456(*)", HkidNumberUtil.maskHkidNumber(hkidNumber));
        assertEquals("*****456(*)", HkidNumberUtil.maskHkidNumber(new HkidNumber("WX123456")));
        assertNull(HkidNumberUtil.maskHkidNumber(null));
    }

    @Test
    void validatesCheckDigit() {
        assertTrue(HkidNumber.validateCheckDigit("A123456", "3"));
        assertTrue(HkidNumber.validateCheckDigit("a123456", "3"));
        assertFalse(HkidNumber.validateCheckDigit("A123456", "7"));
        assertFalse(HkidNumber.validateCheckDigit("A12345", "3"));
        assertFalse(HkidNumber.validateCheckDigit("A1234563", "3"));
        assertFalse(HkidNumber.validateCheckDigit("A123456(3)", "3"));
    }

    @Test
    void exposesDefinedPrefixDescriptionsInBothLanguages() {
        HkidNumber hkidNumber = new HkidNumber("A123456");

        assertEquals(DefinedPrefix.A, hkidNumber.getDefinedPrefix().orElse(null));
        assertEquals(DefinedPrefix.A.getDescription(), hkidNumber.getPrefixDescription());
        assertEquals(DefinedPrefix.A.getTraditionalChineseDescription(),
                hkidNumber.getPrefixTraditionalChineseDescription());
    }

    @Test
    void findsHongKongBirthPrefixFromRegistrationDateAtExactBoundaries() {
        assertFalse(DefinedPrefix
                .fromHongKongBirthRegistrationDate(LocalDate.of(1979, 12, 31)).isPresent());
        assertEquals(DefinedPrefix.Z,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(1980, 1, 1)).orElse(null));
        assertEquals(DefinedPrefix.Z,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(1988, 12, 31)).orElse(null));
        assertEquals(DefinedPrefix.Y,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(1989, 1, 1)).orElse(null));
        assertEquals(DefinedPrefix.Y,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(2005, 3, 31)).orElse(null));
        assertEquals(DefinedPrefix.S,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(2005, 4, 1)).orElse(null));
        assertEquals(DefinedPrefix.S,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(2019, 5, 31)).orElse(null));
        assertEquals(DefinedPrefix.N,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(2019, 6, 1)).orElse(null));
        assertEquals(DefinedPrefix.N,
                DefinedPrefix
                        .fromHongKongBirthRegistrationDate(LocalDate.of(2030, 1, 1)).orElse(null));
    }

    @Test
    void checksFirstIssueMetadataAgainstWholePrintedMonth() {
        assertTrue(DefinedPrefix.K.supportsFirstIssueMonth(YearMonth.of(1983, 3)));
        assertTrue(DefinedPrefix.K.supportsFirstIssueMonth(YearMonth.of(1990, 7)));
        assertFalse(DefinedPrefix.K.supportsFirstIssueMonth(YearMonth.of(1990, 8)));
        assertFalse(DefinedPrefix.K.supportsFirstIssueMonth(YearMonth.of(2004, 11)));
        assertTrue(DefinedPrefix.R.supportsFirstIssueMonth(YearMonth.of(2004, 11)));
        assertArrayEquals(
                new DefinedPrefix[]{DefinedPrefix.R},
                DefinedPrefix.fromFirstIssueMonth(YearMonth.of(2004, 11)));
    }

    @Test
    void usesDescriptionFallbacksForValidUndefinedPrefixes() {
        HkidNumber hkidNumber = new HkidNumber("Q123456");

        assertFalse(hkidNumber.getDefinedPrefix().isPresent());
        assertEquals("No predefined description is available for prefix Q.", hkidNumber.getPrefixDescription());
        assertEquals("字頭 Q 沒有預定義說明。", hkidNumber.getPrefixTraditionalChineseDescription());
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
        HkidNumber hkidNumber = new HkidNumber(prefix, "123456");
        assertDoesNotThrow(hkidNumber::getPrefixDescription);
        assertDoesNotThrow(hkidNumber::getPrefixTraditionalChineseDescription);
    }

    @Test
    void generatesRandomNumber() {
        HkidNumber hkidNumber = HkidNumberUtil.generateRandomHkidNumber();

        assertNotNull(hkidNumber);
        assertDoesNotThrow(() -> new HkidNumber(hkidNumber.toString(HkidNumber.Format.Complete)));

        System.out.println("HKID Number: " + hkidNumber);
        System.out.println("HKID Number (Without Parentheses): " + hkidNumber.toString(HkidNumber.Format.WithoutParentheses));
        System.out.println("HKID Number (Complete): " + hkidNumber.toString(HkidNumber.Format.Complete));
    }

    @Test
    void generatesAsciiNumeralsRegardlessOfDefaultLocale() {
        Locale originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("hi-IN-u-nu-deva"));

            HkidNumber hkidNumber = HkidNumberUtil.generateRandomHkidNumber();

            assertTrue(hkidNumber.getNumerals().matches("[0-9]{6}"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
        }
    }
}
