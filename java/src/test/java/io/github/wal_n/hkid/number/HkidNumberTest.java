package io.github.wal_n.hkid.number;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        assertEquals("A100000(7)", new HkidNumber("A100000").toString(HkidNumber.Format.COMPLETE));
        assertEquals("BC987654(8)", new HkidNumber("BC987654").toString(HkidNumber.Format.COMPLETE));
        assertEquals("C123456(9)", new HkidNumber("C1234569").toString(HkidNumber.Format.COMPLETE));
        assertEquals("DE123456(2)", new HkidNumber("DE1234562").toString(HkidNumber.Format.COMPLETE));
        assertEquals("Z999999(0)", new HkidNumber("Z999999(0)").toString(HkidNumber.Format.COMPLETE));
        assertEquals("A123456(3)", new HkidNumber("A", "123456").toString(HkidNumber.Format.COMPLETE));
        assertEquals("A123456(3)", new HkidNumber("A", "123456", "3").toString(HkidNumber.Format.COMPLETE));
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

    @ParameterizedTest
    @CsvSource({
            "ß, 8",
            "ı, 5",
            "ſ, 2",
            "ﬀ, 9",
            "Aı, 8",
            "ıA, 0",
            "Aſ, 5",
            "ſA, 9",
            "ıſ, A"
    })
    void rejectsUnsupportedUnicodePrefixesBeforeCaseNormalization(String prefix, String checkDigit) {
        String withoutCheckDigit = prefix + "123456";
        // These digits match the ASCII prefixes produced by Unicode uppercasing.
        // A matching checksum must not make an unsupported Unicode prefix valid.
        for (String input : new String[]{
                withoutCheckDigit,
                withoutCheckDigit + checkDigit,
                withoutCheckDigit + "(" + checkDigit + ")",
                withoutCheckDigit + "\uff08" + checkDigit + "\uff09"}) {
            assertThrows(
                    HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber(input), input);
            assertFalse(HkidNumberUtil.isValid(input), input);
        }
        assertThrows(
                HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber(prefix, "123456"));
        assertThrows(
                HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber(prefix, "123456", checkDigit));
        assertFalse(HkidNumberUtil.isValidCheckDigit(withoutCheckDigit, checkDigit));
        assertFalse(HkidNumber.isValidCheckDigit(withoutCheckDigit, checkDigit));
        assertFalse(DefinedPrefix.fromPrefix(" " + prefix + " ").isPresent());
    }

    @ParameterizedTest
    @CsvSource({
            "a, 000002, a, A000002(A)",
            "ab, 123456, 9, AB123456(9)",
            "aB, 123456, 9, AB123456(9)"
    })
    void acceptsAsciiCaseVariations(
            String prefix, String numerals, String checkDigit, String expectedComplete) {
        String withoutCheckDigit = prefix + numerals;
        for (String input : new String[]{
                withoutCheckDigit,
                withoutCheckDigit + checkDigit,
                withoutCheckDigit + "(" + checkDigit + ")",
                withoutCheckDigit + "\uff08" + checkDigit + "\uff09"}) {
            assertEquals(expectedComplete,
                    new HkidNumber(input).toString(HkidNumber.Format.COMPLETE));
            assertTrue(HkidNumberUtil.isValid(input), input);
        }
        assertEquals(expectedComplete,
                new HkidNumber(prefix, numerals).toString(HkidNumber.Format.COMPLETE));
        assertEquals(expectedComplete,
                new HkidNumber(prefix, numerals, checkDigit).toString(HkidNumber.Format.COMPLETE));
        assertTrue(HkidNumberUtil.isValidCheckDigit(withoutCheckDigit, checkDigit));
        assertTrue(HkidNumber.isValidCheckDigit(withoutCheckDigit, checkDigit));
    }

    @Test
    void rawNumberInputsNormalizeEveryFullwidthLatinLetter() {
        String uppercase = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ";
        String lowercase = "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ";
        for (int index = 0; index < uppercase.length(); index++) {
            HkidNumber expected = new HkidNumber(String.valueOf((char) ('A' + index)), "012345");
            for (char prefix : new char[]{uppercase.charAt(index), lowercase.charAt(index)}) {
                String input = prefix + "012345";
                assertEquals(expected, new HkidNumber(input), input);
                assertEquals(expected.toString(), new HkidNumber(input).toString(), input);
                assertTrue(HkidNumberUtil.isValid(input), input);
                assertTrue(HkidNumberUtil.isValidCheckDigit(input, expected.getCheckDigit()), input);
                assertTrue(HkidNumber.isValidCheckDigit(input, expected.getCheckDigit()), input);
            }
        }
    }

    @Test
    void rawNumberInputsNormalizeFullwidthNumeralsAndCheckDigits() {
        String digits = "０１２３４５６７８９";
        for (int index = 0; index < digits.length(); index++) {
            HkidNumber expected = new HkidNumber("A", "00000" + index);
            String body = "Ａ00000" + digits.charAt(index);
            String checkDigit = expected.getCheckDigit();
            String fullwidthCheckDigit = checkDigit.equals("A")
                    ? "Ａ" : String.valueOf(digits.charAt(checkDigit.charAt(0) - '0'));
            assertEquals(expected, new HkidNumber(body + "（" + fullwidthCheckDigit + "）"));
            assertTrue(HkidNumberUtil.isValid(body + fullwidthCheckDigit));
            assertTrue(HkidNumberUtil.isValidCheckDigit(body, fullwidthCheckDigit));
            assertTrue(HkidNumber.isValidCheckDigit(body, fullwidthCheckDigit));
        }
        assertTrue(HkidNumberUtil.isValidCheckDigit("ａ０００００２", "ａ"));
        assertTrue(HkidNumber.isValidCheckDigit("ａ０００００２", "ａ"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"＠123456", "［123456", "｀123456", "｛123456", "A12345／", "A12345："})
    void rejectsPunctuationAdjacentToFullwidthAlphanumericRanges(String input) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber(input));
        assertFalse(HkidNumberUtil.isValid(input));
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

    @ParameterizedTest
    @CsvSource({
            "AB1234569, AB123456(9)",
            "A123456(3), A123456(3)",
            "'\tａ\u3000０００００２\u00a0（ａ）\r', A000002(A)"
    })
    void parsesNumbersWithSuppliedCheckDigits(String input, String expected) {
        assertEquals(expected, HkidNumber.parseWithCheckDigit(input).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"A123456", "\tａ\u3000１２３４５６\u00a0"})
    void reportsMissingCheckDigitsSeparately(String input) {
        assertThrows(HkidNumber.MissingCheckDigitException.class,
                () -> HkidNumber.parseWithCheckDigit(input));
        assertEquals("A123456(3)", new HkidNumber(input).toString());
        assertTrue(HkidNumberUtil.isValid(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A123456(4)", "AB1234560", "Ａ １２３４５６ （ａ）"})
    void reportsIncorrectSuppliedCheckDigits(String input) {
        assertThrows(HkidNumber.InvalidCheckDigitException.class,
                () -> HkidNumber.parseWithCheckDigit(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"\t \u00a0\u3000", "A12345", "A123456()", "A123456(３）", "A123456(B)"})
    void reportsMalformedInputsWhenRequiringACheckDigit(String input) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> HkidNumber.parseWithCheckDigit(input));
    }

    @Test
    void splitConstructorValidatesEachParameterIndependently() {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber(null, "A123456"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A1", "23456"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A", "1234563"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class, () -> new HkidNumber("A", "123456", "33"));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", "\r", "\u00a0", "\u3000"})
    void splitConstructorsDoNotTrimAnyParameter(String whitespace) {
        for (String prefix : new String[]{whitespace + "A", "A" + whitespace}) {
            assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber(prefix, "123456"));
            assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber(prefix, "123456", "3"));
        }
        for (String numerals : new String[]{whitespace + "123456", "123456" + whitespace}) {
            assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber("A", numerals));
            assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber("A", numerals, "3"));
        }
        for (String checkDigit : new String[]{whitespace + "3", "3" + whitespace, whitespace}) {
            assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber("A", "123456", checkDigit));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"\uff11\uff12\uff13\uff14\uff15\uff16", "\u0661\u0662\u0663\u0664\u0665\u0666"})
    void splitConstructorsRejectNonAsciiNumerals(String numerals) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber("A", numerals));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber("A", numerals, "3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ａ", "ａ", "AＢ", "Ａb"})
    void splitConstructorsRejectFullwidthPrefixes(String prefix) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber(prefix, "123456"));
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber(prefix, "123456", "3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\uff13", "\u0663"})
    void splitConstructorRejectsNonAsciiCheckDigits(String checkDigit) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber("A", "123456", checkDigit));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ａ", "ａ"})
    void splitConstructorRejectsFullwidthLetterCheckDigits(String checkDigit) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber("A", "000002", checkDigit));
    }

    @ParameterizedTest
    @ValueSource(strings = {"(3)", "\uff083\uff09", "(3\uff09", "\uff083)"})
    void separateCheckDigitParametersRejectParenthesesOfEitherWidth(String checkDigit) {
        assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                () -> new HkidNumber("A", "123456", checkDigit));
        assertFalse(HkidNumberUtil.isValidCheckDigit("A123456", checkDigit));
        assertFalse(HkidNumber.isValidCheckDigit("A123456", checkDigit));
    }

    @Test
    void splitConstructorStillAllowsAbsentCheckDigit() {
        assertEquals("A123456(3)", new HkidNumber("A", "123456", null).toString());
        assertEquals("A123456(3)", new HkidNumber("A", "123456", "").toString());
    }

    @Test
    void isAnImmutableValueObject() {
        assertTrue(Modifier.isFinal(HkidNumber.class.getModifiers()));
        for (Field field : HkidNumber.class.getDeclaredFields()) {
            assertTrue(Modifier.isFinal(field.getModifiers()), field.getName());
        }
        assertEquals(new HkidNumber("A123456"), new HkidNumber("A123456(3)"));
        assertEquals(new HkidNumber("A123456(3)"), new HkidNumber("A123456\uff083\uff09"));
        assertEquals(
                new HkidNumber("A123456").hashCode(),
                new HkidNumber("A123456(3)").hashCode());
        assertEquals(
                new HkidNumber("A123456(3)").hashCode(),
                new HkidNumber("A123456\uff083\uff09").hashCode());
    }

    @Test
    void fullwidthInputHasTheSameValueAndCanonicalOutputAsAsciiInput() {
        HkidNumber ascii = new HkidNumber("AB123456(9)");
        HkidNumber fullwidth = new HkidNumber(" ａＢ　１2３４5６ （９） ");
        assertEquals(ascii, fullwidth);
        assertEquals(ascii.hashCode(), fullwidth.hashCode());
        assertEquals("AB123456(9)", fullwidth.toString());
        assertEquals("*****456(*)", fullwidth.toMaskedString());
    }

    @Test
    void formatsOutput() {
        HkidNumber hkidNumber = new HkidNumber("A123456(3)");

        assertEquals("A123456(3)", hkidNumber.toString());
        assertEquals("A123456(3)", hkidNumber.toString(null));
        assertEquals("A123456", hkidNumber.toString(HkidNumber.Format.WITHOUT_CHECK_DIGIT));
        assertEquals("A1234563", hkidNumber.toString(HkidNumber.Format.WITHOUT_PARENTHESES));
        assertEquals("A123456(3)", hkidNumber.toString(HkidNumber.Format.COMPLETE));
    }

    @Test
    void masksAllButLastThreeNumerals() {
        HkidNumber hkidNumber = new HkidNumber("A123456(3)");

        assertEquals("****456(*)", hkidNumber.toMaskedString());
        assertEquals("****456(*)", HkidNumberUtil.toMaskedString(hkidNumber));
        assertEquals("*****456(*)", HkidNumberUtil.toMaskedString(new HkidNumber("WX123456")));
        assertNull(HkidNumberUtil.toMaskedString(null));
    }

    @Test
    void validatesCheckDigit() {
        assertTrue(HkidNumberUtil.isValidCheckDigit("A123456", "3"));
        assertTrue(HkidNumberUtil.isValidCheckDigit("a123456", "3"));
        assertFalse(HkidNumberUtil.isValidCheckDigit("A123456", "7"));
        assertFalse(HkidNumberUtil.isValidCheckDigit("A12345", "3"));
        assertFalse(HkidNumberUtil.isValidCheckDigit("A1234563", "3"));
        assertFalse(HkidNumberUtil.isValidCheckDigit("A123456(3)", "3"));

        assertTrue(HkidNumber.isValidCheckDigit("A123456", "3"));
    }

    @Test
    void checkDigitValidationRejectsMissingAndBlankInputs() {
        for (String input : new String[]{null, "", "\t\n\r \u00a0\u3000"}) {
            assertFalse(HkidNumberUtil.isValidCheckDigit(input, "3"));
            assertFalse(HkidNumber.isValidCheckDigit(input, "3"));
            assertFalse(HkidNumberUtil.isValidCheckDigit("A123456", input));
            assertFalse(HkidNumber.isValidCheckDigit("A123456", input));
        }
    }

    @Test
    void validatesWholeNumberWithoutConstructingValueObject() {
        assertTrue(HkidNumberUtil.isValid("A123456"));
        assertTrue(HkidNumberUtil.isValid(" a123456(3) "));
        assertTrue(HkidNumberUtil.isValid("AB123456(9)"));
        assertFalse(HkidNumberUtil.isValid(null));
        assertFalse(HkidNumberUtil.isValid(""));
        assertFalse(HkidNumberUtil.isValid("A123456(7)"));
        assertFalse(HkidNumberUtil.isValid("A12345"));
    }

    @Test
    void validatesAsciiNumberFormatAndOptionalCheckDigit() {
        assertTrue(HkidNumberUtil.isValidAsciiWithoutWhitespace("A123456"));
        assertTrue(HkidNumberUtil.isValidAsciiWithoutWhitespace("ab1234569"));
        assertTrue(HkidNumberUtil.isValidAsciiWithoutWhitespace("a000002(a)"));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace(null));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace(""));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace("A12345"));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace("A123456(4)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\r", "\n", "\u000b", "\f", "\u00a0", "\u3000"})
    void asciiValidationRejectsWhitespaceAnywhere(String whitespace) {
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace(whitespace + "A123456(3)"));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace("A" + whitespace + "123456(3)"));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace("A123456" + whitespace + "(3)"));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace("A123456(3)" + whitespace));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ａ123456(3)", "A12３456(3)", "A123456(３)", "A123456（3）"})
    void asciiValidationRejectsFullwidthCharactersAcceptedByDefault(String input) {
        assertTrue(HkidNumberUtil.isValid(input));
        assertFalse(HkidNumberUtil.isValidAsciiWithoutWhitespace(input));
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
    void definedPrefixLookupPreservesAsciiCaseAndWhitespaceSupport() {
        assertEquals(DefinedPrefix.S, DefinedPrefix.fromPrefix(" s ").orElse(null));
        assertEquals(DefinedPrefix.WX, DefinedPrefix.fromPrefix(" wX ").orElse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ＷＸ", "ｗｘ", "wＸ", "Ｗx", "\tｗＸ\u3000"})
    void definedPrefixLookupNormalizesFullwidthAndMixedWidthLetters(String prefix) {
        assertEquals(DefinedPrefix.WX, DefinedPrefix.fromPrefix(prefix).orElse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", "\r", "\u00a0", "\u3000"})
    void definedPrefixLookupOnlyRemovesWhitespaceAtTheEdges(String whitespace) {
        assertEquals(DefinedPrefix.WX, DefinedPrefix.fromPrefix(whitespace + "wX").orElse(null));
        assertEquals(DefinedPrefix.WX, DefinedPrefix.fromPrefix("wX" + whitespace).orElse(null));
        assertFalse(DefinedPrefix.fromPrefix("W" + whitespace + "X").isPresent());
        assertFalse(DefinedPrefix.fromPrefix(whitespace).isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\0", "\u000b", "\f", "\u001f", "\u0085", "\u2003", "\u200b", "\ufeff"})
    void definedPrefixLookupRejectsOtherControlsAndUnicodeWhitespace(String character) {
        assertFalse(DefinedPrefix.fromPrefix(character + "WX").isPresent());
        assertFalse(DefinedPrefix.fromPrefix("WX" + character).isPresent());
    }

    @Test
    void definedPrefixLookupRejectsNullAndEmptyInput() {
        assertFalse(DefinedPrefix.fromPrefix(null).isPresent());
        assertFalse(DefinedPrefix.fromPrefix("").isPresent());
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
    void boundsFirstIssueMonthByExactFToLTransitionDate() {
        YearMonth july = YearMonth.of(2026, 7);
        LocalDate dayBeforeTransition = LocalDate.of(2026, 7, 26);
        LocalDate transitionDate = LocalDate.of(2026, 7, 27);

        assertTrue(DefinedPrefix.F.supportsFirstIssueMonth(july, dayBeforeTransition));
        assertFalse(DefinedPrefix.L.supportsFirstIssueMonth(july, dayBeforeTransition));
        assertTrue(DefinedPrefix.L.supportsFirstIssueMonth(july, transitionDate));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F},
                DefinedPrefix.fromFirstIssueMonth(july, dayBeforeTransition));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F, DefinedPrefix.L},
                DefinedPrefix.fromFirstIssueMonth(july, transitionDate));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F, DefinedPrefix.L},
                DefinedPrefix.fromFirstIssueMonth(july));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F, DefinedPrefix.L},
                DefinedPrefix.fromFirstIssueMonth(july, LocalDate.of(2026, 8, 1)));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.L},
                DefinedPrefix.fromFirstIssueMonth(
                        YearMonth.of(2026, 8), LocalDate.of(2026, 8, 1)));
    }

    @Test
    void boundsFirstIssueMonthAcrossEarlierMidMonthTransitions() {
        YearMonth february = YearMonth.of(2020, 2);

        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.M},
                DefinedPrefix.fromFirstIssueMonth(february, LocalDate.of(2020, 2, 23)));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F, DefinedPrefix.M},
                DefinedPrefix.fromFirstIssueMonth(february, LocalDate.of(2020, 2, 24)));
    }

    @Test
    void boundedFirstIssueLookupReturnsNoMatchesForMissingOrEmptyDateRanges() {
        YearMonth july = YearMonth.of(2026, 7);
        LocalDate transitionDate = LocalDate.of(2026, 7, 27);
        LocalDate beforeMonth = LocalDate.of(2026, 6, 30);

        assertFalse(DefinedPrefix.L.supportsFirstIssueMonth(null, transitionDate));
        assertFalse(DefinedPrefix.L.supportsFirstIssueMonth(july, null));
        assertFalse(DefinedPrefix.F.supportsFirstIssueMonth(july, beforeMonth));
        assertFalse(DefinedPrefix.J.supportsFirstIssueMonth(july, transitionDate));
        assertFalse(DefinedPrefix.N.supportsFirstIssueMonth(july, transitionDate));
        assertArrayEquals(new DefinedPrefix[0], DefinedPrefix.fromFirstIssueMonth(null, transitionDate));
        assertArrayEquals(new DefinedPrefix[0], DefinedPrefix.fromFirstIssueMonth(july, null));
        assertArrayEquals(new DefinedPrefix[0], DefinedPrefix.fromFirstIssueMonth(july, beforeMonth));
        assertArrayEquals(new DefinedPrefix[0], DefinedPrefix.fromFirstIssueMonth(july, null, transitionDate));
        assertArrayEquals(new DefinedPrefix[0], DefinedPrefix.fromFirstIssueMonth(july, transitionDate, null));
        assertArrayEquals(new DefinedPrefix[0],
                DefinedPrefix.fromFirstIssueMonth(july, transitionDate, transitionDate.minusDays(1)));
        assertArrayEquals(new DefinedPrefix[0],
                DefinedPrefix.fromFirstIssueMonth(july, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)));
    }

    @Test
    void usesDescriptionFallbacksForValidUndefinedPrefixes() {
        HkidNumber hkidNumber = new HkidNumber("Q123456");

        assertFalse(hkidNumber.getDefinedPrefix().isPresent());
        assertTrue(hkidNumber.getPrefixDescription().contains(hkidNumber.getPrefix()));
        assertTrue(hkidNumber.getPrefixTraditionalChineseDescription()
                .contains(hkidNumber.getPrefix()));
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
    void definedPrefixGenerationRespectsAllowedPrefixes() {
        HkidNumber defined = HkidNumberUtil.generateRandomHkidNumber();
        assertGeneratedNumber(defined);
        assertTrue(defined.getDefinedPrefix().isPresent());
        HkidNumber restricted = HkidNumberUtil.generateRandomHkidNumber(DefinedPrefix.Z);
        assertGeneratedNumber(restricted);
        assertEquals("Z", restricted.getPrefix());
    }

    @Test
    void arbitraryPrefixGenerationProducesBothLengthsUndefinedPrefixesAndLeadingZeros() {
        boolean sawOneLetter = false;
        boolean sawTwoLetters = false;
        boolean sawUndefinedPrefix = false;
        boolean sawLeadingZero = false;
        for (int i = 0; i < 1024; i++) {
            HkidNumber number = HkidNumberUtil.generateRandomHkidNumber(false);
            assertGeneratedNumber(number);
            sawOneLetter |= number.getPrefix().length() == 1;
            sawTwoLetters |= number.getPrefix().length() == 2;
            sawUndefinedPrefix |= !number.getDefinedPrefix().isPresent();
            sawLeadingZero |= number.getNumerals().startsWith("0");
            if (sawOneLetter && sawTwoLetters && sawUndefinedPrefix && sawLeadingZero) {
                break;
            }
        }
        assertTrue(sawOneLetter, "Expected a one-letter prefix");
        assertTrue(sawTwoLetters, "Expected a two-letter prefix");
        assertTrue(sawUndefinedPrefix, "Expected a prefix outside DefinedPrefix");
        assertTrue(sawLeadingZero, "Expected six numerals starting with zero");
    }

    @Test
    void randomNumberGenerationRejectsInvalidAllowedPrefixes() {
        assertThrows(IllegalArgumentException.class,
                () -> HkidNumberUtil.generateRandomHkidNumber(DefinedPrefix.A, null));
        assertThrows(IllegalArgumentException.class,
                () -> HkidNumberUtil.generateRandomHkidNumber(new DefinedPrefix[0]));
        assertThrows(IllegalArgumentException.class,
                () -> HkidNumberUtil.generateRandomHkidNumber((DefinedPrefix[]) null));
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

    private static void assertGeneratedNumber(HkidNumber hkidNumber) {
        assertNotNull(hkidNumber);
        assertTrue(hkidNumber.getPrefix().matches("[A-Z]{1,2}"));
        assertTrue(hkidNumber.getNumerals().matches("[0-9]{6}"));
        assertTrue(hkidNumber.getCheckDigit().matches("[0-9A]"));
        assertEquals(
                hkidNumber,
                HkidNumber.parseWithCheckDigit(hkidNumber.toString(HkidNumber.Format.COMPLETE)));
    }
}
