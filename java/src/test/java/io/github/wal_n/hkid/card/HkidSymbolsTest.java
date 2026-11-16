package io.github.wal_n.hkid.card;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
        for (HkidSymbol symbol : HkidSymbol.values()) {
            assertFalse(symbol.getDescription().isEmpty());
            assertFalse(symbol.getTraditionalChineseDescription().isEmpty());
        }
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

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", "\r", "\u00a0", "\u3000"})
    void singleSymbolParserOnlyRemovesWhitespaceAtTheEdges(String whitespace) {
        assertEquals(HkidSymbol.RIGHT_OF_ABODE, HkidSymbol.fromCode(whitespace + "a"));
        assertEquals(HkidSymbol.RIGHT_OF_ABODE, HkidSymbol.fromCode(whitespace + "ａ" + whitespace));
        assertEquals(HkidSymbol.ADULT_RE_ENTRY_PERMIT, HkidSymbol.fromCode("***" + whitespace));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode("*" + whitespace + "**"));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode(whitespace));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\0", "\u000b", "\f", "\u001f", "\u0085", "\u2003", "\u200b", "\ufeff"})
    void singleSymbolParserRejectsOtherControlsAndUnicodeWhitespace(String character) {
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode(character + "A"));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode("A" + character));
    }

    @Test
    void singleSymbolParserRejectsMissingInputAndMultipleTokens() {
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode(null));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode(""));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode("*** A"));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode("* * *"));
    }

    @ParameterizedTest
    @CsvSource({"Ａ, A", "ａ, A", "Ｚ, Z", "ｚ, Z", "Ｎ, N", "ｎ, N"})
    void singleSymbolParserNormalizesFullwidthLetterCase(String input, String asciiCode) {
        assertEquals(HkidSymbol.fromCode(asciiCode), HkidSymbol.fromCode(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"＊", "＊＊＊", "Ⓐ", "\ud835\udc00"})
    void symbolParsersDoNotApplyGeneralCompatibilityNormalization(String input) {
        assertThrows(IllegalArgumentException.class, () -> HkidSymbol.fromCode(input));
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.parse(input));
        assertFalse(HkidSymbolsUtil.isValid(input));
    }

    @Test
    void mixedWidthSymbolsHaveTheSameValueAndCanonicalOutputAsAsciiSymbols() {
        HkidSymbols ascii = HkidSymbols.parse("***AZBN");
        HkidSymbols mixed = HkidSymbols.parse("ｎ b Ｚ *** ａ");
        assertEquals(ascii, mixed);
        assertEquals(ascii.hashCode(), mixed.hashCode());
        assertEquals("***AZBN", mixed.toString());
        assertTrue(HkidSymbolsUtil.isValid("ｎ b Ｚ *** ａ"));
    }

    @Test
    void reportsWhetherSymbolCodesAreValid() {
        assertTrue(HkidSymbolsUtil.isValid("***AZ"));
        assertTrue(HkidSymbolsUtil.isValid(" nbz***a "));
        assertTrue(HkidSymbolsUtil.isValid(" "));
        assertFalse(HkidSymbolsUtil.isValid(null));
        assertFalse(HkidSymbolsUtil.isValid("***A*"));
        assertFalse(HkidSymbolsUtil.isValid("AA"));
        assertFalse(HkidSymbolsUtil.isValid("Y"));
        assertFalse(HkidSymbolsUtil.isValid("H1"));
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
        assertTrue(HkidSymbols.parse("").isEmpty());
        assertTrue(HkidSymbols.parse(" ").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> HkidSymbols.parse(null));
        assertEquals("BN", HkidSymbols.of(
                HkidSymbol.BIRTH_DETAILS_CHANGED, HkidSymbol.NAME_CHANGED).toString());
    }
}
