package io.github.wal_n.hkid.card;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidSymbolsTest {
    @Test
    void exposesOfficialSymbolMetadata() {
        assertEquals("***", HkidSymbol.ADULT_RE_ENTRY_PERMIT.getCode());
        assertEquals("***", HkidSymbol.ADULT_RE_ENTRY_PERMIT.toString());
        assertEquals(HkidSymbolCategory.RESIDENTIAL_STATUS,
                HkidSymbol.RIGHT_OF_ABODE.getCategory());
        assertEquals(
                "The holder has the right of abode in the HKSAR.",
                HkidSymbol.RIGHT_OF_ABODE.getDescription());
        assertEquals(
                "持證人擁有香港居留權",
                HkidSymbol.RIGHT_OF_ABODE.getTraditionalChineseDescription());
    }

    @Test
    void parsesAndUsesCanonicalOrder() {
        HkidSymbols symbols = HkidSymbols.parse("nbz***a");

        assertEquals("***AZBN", symbols.toString());
        assertEquals(HkidSymbol.ADULT_RE_ENTRY_PERMIT, symbols.asList().get(0));
        assertEquals(Arrays.asList(HkidSymbol.BIRTH_DETAILS_CHANGED, HkidSymbol.NAME_CHANGED),
                symbols.getByCategory(HkidSymbolCategory.OTHER_INFORMATION));
        assertEquals(HkidSymbol.RIGHT_OF_ABODE, HkidSymbol.fromCode("a"));
    }

    @Test
    void isImmutable() {
        HkidSymbols symbols = HkidSymbols.parse("***AZ");

        assertThrows(UnsupportedOperationException.class,
                () -> symbols.asList().add(HkidSymbol.NAME_CHANGED));
        assertThrows(UnsupportedOperationException.class,
                () -> symbols.getByCategory(HkidSymbolCategory.RESIDENTIAL_STATUS).clear());
    }

    @Test
    void rejectsConflictsDuplicatesAndLegacyCodes() {
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.of(
                HkidSymbol.ADULT_RE_ENTRY_PERMIT, HkidSymbol.MINOR_RE_ENTRY_PERMIT));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.of(
                HkidSymbol.RIGHT_OF_ABODE, HkidSymbol.STAY_LIMITED));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.of(
                HkidSymbol.BORN_IN_HONG_KONG, HkidSymbol.BORN_IN_MAINLAND));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.of(
                HkidSymbol.NAME_CHANGED, HkidSymbol.NAME_CHANGED));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.parse("Y"));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.parse("H1"));
    }

    @Test
    void supportsEmptySymbolsAndIndependentOtherInformation() {
        assertTrue(HkidSymbols.parse(" ").isEmpty());
        assertEquals("BN", HkidSymbols.of(
                HkidSymbol.BIRTH_DETAILS_CHANGED, HkidSymbol.NAME_CHANGED).toString());
    }
}
