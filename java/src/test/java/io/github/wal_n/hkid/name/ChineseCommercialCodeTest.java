package io.github.wal_n.hkid.name;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
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
        assertTrue(ChineseNameUtil.isValidCommercialCode(value));
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
        assertFalse(ChineseNameUtil.isValidCommercialCode(value));
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
