package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonObject;
import io.github.wal_n.hkid.internal.ResourceJson;
import io.github.wal_n.hkid.number.DefinedPrefix;
import io.github.wal_n.hkid.number.HkidNumber;
import io.github.wal_n.hkid.number.HkidNumberUtil;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidNumberConformanceTest {
    private static final String FIXTURE = "hkid-number.json";

    @TestFactory
    Stream<DynamicTest> parsesAndFormatsSharedCases() {
        return ConformanceFixtures.cases(FIXTURE, "parseCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertParsedNumber(testCase)));
    }

    @TestFactory
    Stream<DynamicTest> rejectsSharedInvalidCases() {
        return ConformanceFixtures.cases(FIXTURE, "invalidParseCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertInvalidNumber(testCase)));
    }

    @TestFactory
    Stream<DynamicTest> validatesSharedCheckDigitCases() {
        return ConformanceFixtures.cases(FIXTURE, "checkDigitValidationCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> {
                            String input = ConformanceFixtures.requireNullableString(testCase, "input");
                            String checkDigit = ConformanceFixtures.requireNullableString(testCase, "checkDigit");
                            boolean expectValid = testCase.get("expectValid").getAsBoolean();
                            assertAll(
                                    () -> assertEquals(expectValid,
                                            HkidNumberUtil.isValidCheckDigit(input, checkDigit)),
                                    () -> assertEquals(expectValid,
                                            HkidNumber.isValidCheckDigit(input, checkDigit)));
                        }));
    }

    @TestFactory
    Stream<DynamicTest> describesSharedNumberPrefixesInBothLanguages() {
        return ConformanceFixtures.cases(FIXTURE, "prefixDescriptionCases")
                .map(testCase -> {
                    ResourceJson.requireExactKeys(testCase, FIXTURE,
                            "id", "input", "expectEnglish", "expectTraditionalChinese");
                    String id = ResourceJson.requireString(testCase, "id", FIXTURE);
                    return DynamicTest.dynamicTest(id, () -> {
                        HkidNumber number = new HkidNumber(ResourceJson.requireString(testCase, "input", id));
                        assertAll(
                                () -> assertEquals(ResourceJson.requireString(testCase, "expectEnglish", id),
                                        number.getPrefixDescription()),
                                () -> assertEquals(ResourceJson.requireString(testCase, "expectTraditionalChinese", id),
                                        number.getPrefixTraditionalChineseDescription()));
                    });
                });
    }

    @TestFactory
    Stream<DynamicTest> resolvesSharedBirthRegistrationPrefixes() {
        return ConformanceFixtures.cases(FIXTURE, "birthRegistrationPrefixCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertEquals(
                                ConformanceFixtures.nullableString(testCase, "expectPrefix"),
                                DefinedPrefix.fromHongKongBirthRegistrationDate(
                                                LocalDate.parse(testCase.get("date").getAsString()))
                                        .map(Enum::name)
                                        .orElse(null))));
    }

    @TestFactory
    Stream<DynamicTest> resolvesSharedFirstIssuePrefixes() {
        return ConformanceFixtures.cases(FIXTURE, "firstIssuePrefixCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> {
                            List<String> actual = Arrays.stream(
                                            DefinedPrefix.fromFirstIssueMonth(
                                                    YearMonth.parse(testCase.get("month").getAsString())))
                                    .map(Enum::name)
                                    .collect(Collectors.toList());
                            assertEquals(
                                    ConformanceFixtures.strings(testCase, "expectPrefixes"),
                                    actual);
                        }));
    }

    private static void assertParsedNumber(JsonObject testCase) {
        String input = ConformanceFixtures.requireNullableString(testCase, "input");
        HkidNumber number = new HkidNumber(input);
        JsonObject expected = testCase.getAsJsonObject("expect");

        assertAll(
                () -> assertTrue(HkidNumberUtil.isValid(input)),
                () -> assertEquals(expected.get("prefix").getAsString(), number.getPrefix()),
                () -> assertEquals(expected.get("numerals").getAsString(), number.getNumerals()),
                () -> assertEquals(expected.get("checkDigit").getAsString(), number.getCheckDigit()),
                () -> assertEquals(
                        expected.get("withoutCheckDigit").getAsString(),
                        number.toString(HkidNumber.Format.WITHOUT_CHECK_DIGIT)),
                () -> assertEquals(expected.get("complete").getAsString(), number.toString()),
                () -> assertEquals(
                        expected.get("withoutParentheses").getAsString(),
                        number.toString(HkidNumber.Format.WITHOUT_PARENTHESES)),
                () -> assertEquals(
                        expected.get("complete").getAsString(),
                        number.toString(HkidNumber.Format.COMPLETE)),
                () -> assertEquals(expected.get("masked").getAsString(), number.toMaskedString()));
    }

    private static void assertInvalidNumber(JsonObject testCase) {
        String input = ConformanceFixtures.requireNullableString(testCase, "input");
        String expectedError = testCase.get("expectError").getAsString();

        assertFalse(HkidNumberUtil.isValid(input));

        if ("INVALID_CHECK_DIGIT".equals(expectedError)) {
            assertThrows(HkidNumber.InvalidCheckDigitException.class,
                    () -> new HkidNumber(input));
            return;
        }
        if ("INVALID_FORMAT".equals(expectedError)) {
            assertThrows(HkidNumber.InvalidHkidNumberFormatException.class,
                    () -> new HkidNumber(input));
            return;
        }
        throw new AssertionError("Unsupported shared error code " + expectedError);
    }
}
