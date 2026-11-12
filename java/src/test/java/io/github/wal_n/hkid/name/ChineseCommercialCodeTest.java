package io.github.wal_n.hkid.name;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChineseCommercialCodeTest {
    @ParameterizedTest
    @ValueSource(strings = {"0000", "0001", "0123", "7115", "9999"})
    void preservesAllFourDigits(String value) {
        ChineseCommercialCode code = new ChineseCommercialCode(value);

        assertEquals(value, code.getCode());
        assertEquals(value, code.toString());
        assertEquals(Optional.empty(), code.getExtensionNumber());
        assertTrue(ChineseNameUtil.isValidCommercialCode(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"})
    void preservesExtensionSeparatelyFromFourDigitDisplay(String extensionNumber) {
        ChineseCommercialCode code = new ChineseCommercialCode("0123", extensionNumber);

        assertEquals("0123", code.getCode());
        assertEquals("0123", code.toString());
        assertEquals(Optional.of(extensionNumber), code.getExtensionNumber());
    }

    @Test
    void nullExtensionIsTheSameAsOmittingIt() {
        ChineseCommercialCode code = new ChineseCommercialCode("2479", null);
        ChineseCommercialCode withoutExtension = new ChineseCommercialCode("2479");

        assertEquals(Optional.empty(), code.getExtensionNumber());
        assertEquals(withoutExtension, code);
        assertEquals(withoutExtension.hashCode(), code.hashCode());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", " 2", "2 ", "2\n", "\t", "22", "+2", "-2", "2.0", "A", "\uFF12", "\u0662"})
    void rejectsInvalidExtensions(String extensionNumber) {
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseCommercialCode("2479", extensionNumber));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "0", "123", "12345", "abcd", "12a4", "+123", "-123",
            " 1234", "1234 ", "12 4", "1234\n", "12.3",
            "\uFF11\uFF12\uFF13\uFF14", "\u0661\u0662\u0663\u0664"
    })
    void rejectsInvalidCodes(String value) {
        assertThrows(IllegalArgumentException.class, () -> new ChineseCommercialCode(value));
        assertThrows(IllegalArgumentException.class, () -> new ChineseCommercialCode(value, "2"));
        assertFalse(ChineseNameUtil.isValidCommercialCode(value));
    }

    @Test
    void extensionsDistinguishValuesInCollectionsDespiteIdenticalDisplay() {
        ChineseCommercialCode withoutExtension = new ChineseCommercialCode("2479");
        ChineseCommercialCode zeroExtension = new ChineseCommercialCode("2479", "0");
        ChineseCommercialCode secondExtension = new ChineseCommercialCode("2479", "2");
        ChineseCommercialCode sameSecondExtension = new ChineseCommercialCode("2479", "2");
        Set<ChineseCommercialCode> codes = new HashSet<>();
        codes.add(withoutExtension);
        codes.add(zeroExtension);
        codes.add(secondExtension);
        codes.add(sameSecondExtension);

        assertEquals(secondExtension, sameSecondExtension);
        assertEquals(sameSecondExtension, secondExtension);
        assertEquals(secondExtension.hashCode(), sameSecondExtension.hashCode());
        assertNotEquals(withoutExtension, zeroExtension);
        assertNotEquals(withoutExtension, secondExtension);
        assertNotEquals(zeroExtension, secondExtension);
        assertEquals(3, codes.size());
        assertEquals(withoutExtension.toString(), zeroExtension.toString());
        assertEquals(withoutExtension.toString(), secondExtension.toString());
    }

    @Test
    void equalCodesActAsTheSameValueInCollections() {
        ChineseCommercialCode code = new ChineseCommercialCode("0001");
        ChineseCommercialCode sameCode = new ChineseCommercialCode("0001");
        Set<ChineseCommercialCode> codes = new HashSet<>();
        codes.add(code);
        codes.add(sameCode);
        codes.add(new ChineseCommercialCode("0002"));

        assertEquals(code, sameCode);
        assertEquals(sameCode, code);
        assertEquals(code.hashCode(), sameCode.hashCode());
        assertEquals(2, codes.size());
        assertNotEquals(code, new ChineseCommercialCode("0002"));
        assertNotEquals(code, "0001");
        assertNotEquals(code, null);
    }
}
