package test.java;

import main.java.HKIDNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HKIDNumberTest {

    @Test
    void constructorValidInputTest() {
        assertDoesNotThrow(() -> new HKIDNumber("A100000"));
        assertDoesNotThrow(() -> new HKIDNumber("BC987654"));
        assertDoesNotThrow(() -> new HKIDNumber("C1234569"));
        assertDoesNotThrow(() -> new HKIDNumber("DE1234562"));
        assertDoesNotThrow(() -> new HKIDNumber("F123456(7)"));
        assertDoesNotThrow(() -> new HKIDNumber("ZZ999999(9)"));
    }

    @Test
    void constructorInvalidFormatTest() {
        // Prefix
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("01234567"));
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("#$123456"));

        // Numerals
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("AB12345(9)"));
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("AB1234567(0)"));
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("AB1C34567(9)"));

        //
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("AB1234567((9)"));
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("AB1234567(9"));
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> new HKIDNumber("AB12345679)"));
    }

    @Test
    void constructorInvalidCheckDigitTest() {
        assertThrows(HKIDNumber.InvalidCheckDigitException.class, () -> new HKIDNumber("A123456(7)"));
        assertThrows(HKIDNumber.InvalidCheckDigitException.class, () -> new HKIDNumber("AB123456(0)"));
    }

    @Test
    void setAndGetPrefixTest() {
        HKIDNumber hkid = new HKIDNumber("A123456(3)");
        hkid.setPrefix("B");
        assertEquals("B", hkid.getPrefix());
        assertEquals("6", hkid.getCheckDigit());
    }

    @Test
    void setAndGetNumeralsTest() {
        HKIDNumber hkid = new HKIDNumber("A123456(3)");
        hkid.setNumerals("654321");
        assertEquals("654321", hkid.getNumerals());
        assertEquals("1", hkid.getCheckDigit());
    }

    @Test
    void setInvalidPrefixTest() {
        HKIDNumber hkid = new HKIDNumber("A123456(3)");
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> hkid.setPrefix("1"));
    }

    @Test
    void setInvalidNumeralsTest() {
        HKIDNumber hkid = new HKIDNumber("A123456(3)");
        assertThrows(HKIDNumber.InvalidHKIDNumberFormatException.class, () -> hkid.setNumerals("ABCDE"));
    }

    @Test
    void checkDigitCalculationTest() {
        HKIDNumber hkid = new HKIDNumber("A123456");
        assertEquals("3", hkid.getCheckDigit());
    }

    @Test
    void toStringFormatTest() {
        HKIDNumber hkid = new HKIDNumber("A123456(3)");
        assertEquals("A123456", hkid.toString(HKIDNumber.Format.WithoutCheckDigit));
        assertEquals("A1234563", hkid.toString(HKIDNumber.Format.WithoutParentheses));
        assertEquals("A123456(3)", hkid.toString(HKIDNumber.Format.Complete));
    }

    @Test
    void genRandomHkidNumberTest() {
        HKIDNumber hkid = HKIDNumber.genRandomHkidNumber();
        assertNotNull(hkid);
    }

    @Test
    void validateCheckDigitTest() {
        assertTrue(HKIDNumber.validateCheckDigit("A123456", "3"));
        assertTrue(!HKIDNumber.validateCheckDigit("A123456", "7"));
        assertFalse(HKIDNumber.validateCheckDigit("A12345", "3"));
        assertFalse(HKIDNumber.validateCheckDigit("A1234563", "3"));
        assertFalse(HKIDNumber.validateCheckDigit("A123456(3)", "3"));
    }
}