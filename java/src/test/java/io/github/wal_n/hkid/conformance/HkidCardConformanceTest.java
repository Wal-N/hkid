package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.HkidCardUtil;
import io.github.wal_n.hkid.card.HkidSuspicion;
import io.github.wal_n.hkid.card.Sex;
import io.github.wal_n.hkid.name.ChineseCommercialCode;
import io.github.wal_n.hkid.number.HkidNumber;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ResourceLock("java.util.Locale.default")
class HkidCardConformanceTest {
    private static final String FIXTURE = "hkid-card.json";

    @TestFactory
    Stream<DynamicTest>constructsAndFormatsSharedCardCases() {
        return ConformanceFixtures.cases(FIXTURE, "cardCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertCard(testCase)));
    }

    @TestFactory
    Stream<DynamicTest>calculatesSharedAgeCases() {
        return ConformanceFixtures.cases(FIXTURE, "ageCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertAge(testCase)));
    }

    @TestFactory
    Stream<DynamicTest>rejectsSharedInvalidCardCases() {
        return ConformanceFixtures.cases(FIXTURE, "invalidCardCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertThrows(
                                IllegalArgumentException.class,
                                () -> buildCard(testCase))));
    }

    @TestFactory
    Stream<DynamicTest>validatesSharedAsOfCases() {
        return ConformanceFixtures.cases(FIXTURE, "asOfValidationCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertAsOfValidation(testCase)));
    }

    @TestFactory
    Stream<DynamicTest>findsSharedSuspicionCases() {
        return ConformanceFixtures.cases(FIXTURE, "suspicionCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertSuspicions(testCase)));
    }

    @TestFactory
    Stream<DynamicTest>describesSharedHolderCases() {
        return ConformanceFixtures.cases(FIXTURE, "holderDescriptionCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertHolderDescription(testCase)));
    }

    private static void assertCard(JsonObject testCase) {
        HkidCard card = buildCard(testCase.getAsJsonObject("input"));
        JsonObject expected = testCase.getAsJsonObject("expect");
        String hkidComplete = expected.get("hkidComplete").getAsString();
        LocalDate referenceDate = LocalDate.parse(
                testCase.get("referenceDate").getAsString());

        assertAll(
                () -> assertEquals(hkidComplete, card.getHkidNumber().toString()),
                () -> assertEquals(hkidComplete, card.getHkidNumberString()),
                () -> assertEquals(hkidComplete, card.getHkidNumberString(null)),
                () -> assertEquals(
                        expected.get("hkidWithoutCheckDigit").getAsString(),
                        card.getHkidNumberString(HkidNumber.Format.WITHOUT_CHECK_DIGIT)),
                () -> assertEquals(
                        hkidComplete,
                        card.getHkidNumberString(HkidNumber.Format.COMPLETE)),
                () -> assertEquals(
                        expected.get("hkidMasked").getAsString(),
                        card.getMaskedHkidNumberString()),
                () -> assertEquals(
                        expected.get("chineseName").getAsString(),
                        card.getChineseNameString()),
                () -> assertEquals(
                        expected.get("englishName").getAsString(),
                        card.getEnglishNameString()),
                () -> assertEquals(
                        expected.get("sexPrintedValue").getAsString(),
                        card.getSexString()),
                () -> assertEquals(
                        expected.get("dateOfBirth").getAsString(),
                        card.getDateOfBirthString()),
                () -> assertEquals(
                        expected.get("symbols").getAsString(),
                        card.getSymbolsString()),
                () -> assertEquals(
                        expected.get("firstRegistrationYearMonth").getAsString(),
                        card.getFirstRegistrationYearMonthString()),
                () -> assertEquals(
                        expected.get("dateOfRegistration").getAsString(),
                        card.getDateOfRegistrationString()),
                () -> assertEquals(
                        Integer.valueOf(expected.get("age").getAsInt()),
                        card.getAge(referenceDate).orElse(null)),
                () -> assertDoesNotThrow(() -> card.validateAsOf(referenceDate)));

        if (expected.has("chineseCommercialCodes")) {
            assertCommercialCodes(expected.getAsJsonArray("chineseCommercialCodes"),
                    card.getChineseCommercialCodes());
            assertCommercialCodes(expected.getAsJsonArray("chineseCommercialCodes"),
                    card.getChineseName().getCommercialCodes());
        }
    }

    private static void assertCommercialCodes(
            JsonArray expected, List<ChineseCommercialCode> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            JsonObject expectedCode = expected.get(index).getAsJsonObject();
            ChineseCommercialCode actualCode = actual.get(index);
            assertAll("Commercial code " + index,
                    () -> assertEquals(expectedCode.get("code").getAsString(),
                            actualCode.getCode()),
                    () -> assertEquals(
                            ConformanceFixtures.nullableString(expectedCode, "extensionNumber"),
                            actualCode.getExtensionNumber().orElse(null)),
                    () -> assertEquals(expectedCode.get("display").getAsString(),
                            actualCode.toString()));
        }
    }

    private static void assertAge(JsonObject testCase) {
        HkidCard card = HkidCard.builder()
                .dateOfBirth(LocalDate.parse(
                        testCase.get("dateOfBirth").getAsString()))
                .build();
        LocalDate referenceDate = LocalDate.parse(
                testCase.get("referenceDate").getAsString());

        assertEquals(
                Integer.valueOf(testCase.get("expectAge").getAsInt()),
                card.getAge(referenceDate).orElse(null));
    }

    private static void assertAsOfValidation(JsonObject testCase) {
        HkidCard card = buildCard(testCase);
        LocalDate referenceDate = LocalDate.parse(
                testCase.get("referenceDate").getAsString());

        if (testCase.get("expectValid").getAsBoolean()) {
            assertDoesNotThrow(() -> card.validateAsOf(referenceDate));
        } else {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> card.validateAsOf(referenceDate));
        }
    }

    private static void assertSuspicions(JsonObject testCase) {
        HkidCard card = buildCard(testCase.getAsJsonObject("input"));
        List<String> expected = ConformanceFixtures.strings(testCase, "expectReasons");
        assertAll(
                () -> assertEquals(expected, card.findSuspicions().stream()
                        .map(HkidSuspicion::name).collect(Collectors.toList())),
                () -> assertEquals(expected, HkidCardUtil.findSuspicions(card).stream()
                        .map(HkidSuspicion::name).collect(Collectors.toList())),
                () -> assertEquals(!expected.isEmpty(), card.isSuspicious()),
                () -> assertEquals(!expected.isEmpty(), HkidCardUtil.isSuspicious(card)));
    }

    private static void assertHolderDescription(JsonObject testCase) {
        HkidCard card = buildCard(testCase.getAsJsonObject("input"));
        LocalDate referenceDate = LocalDate.parse(testCase.get("referenceDate").getAsString());
        String languageTag = ConformanceFixtures.nullableString(testCase, "locale");
        String defaultLanguageTag = ConformanceFixtures.nullableString(testCase, "defaultLocale");
        if (languageTag == null && defaultLanguageTag == null) {
            throw new IllegalStateException("Holder description needs locale or defaultLocale");
        }
        Locale locale = languageTag == null ? null : Locale.forLanguageTag(languageTag);
        String expected = testCase.get("expectDescription").getAsString();
        Locale original = Locale.getDefault();
        Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
        try {
            if (defaultLanguageTag != null) {
                Locale.setDefault(Locale.forLanguageTag(defaultLanguageTag));
            }
            assertAll(
                    () -> assertEquals(expected, card.describeHolder(locale, referenceDate)),
                    () -> assertEquals(expected,
                            HkidCardUtil.describeHolder(card, locale, referenceDate)));
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT, originalFormat);
        }
    }

    private static HkidCard buildCard(JsonObject input) {
        HkidCard.Builder builder = HkidCard.builder();
        String hkidNumber = ConformanceFixtures.nullableString(input, "hkidNumber");
        if (hkidNumber != null) {
            builder.hkidNumber(new HkidNumber(hkidNumber));
        }
        builder.chineseSurname(ConformanceFixtures.nullableString(input, "chineseSurname"))
                .chinesePersonalName(ConformanceFixtures.nullableString(input, "chinesePersonalName"))
                .englishSurname(ConformanceFixtures.nullableString(input, "englishSurname"))
                .englishPersonalName(ConformanceFixtures.nullableString(input, "englishPersonalName"));
        if (input.has("chineseCommercialCodes") && !input.get("chineseCommercialCodes").isJsonNull()) {
            builder.chineseCommercialCodes(
                    ConformanceFixtures.commercialCodes(input, "chineseCommercialCodes"));
        }
        String sex = ConformanceFixtures.nullableString(input, "sex");
        if (sex != null) {
            builder.sex(Sex.fromEnglishMarker(sex));
        }
        String symbols = ConformanceFixtures.nullableString(input, "symbols");
        if (symbols != null) {
            builder.symbolCodes(symbols);
        }

        String dateOfBirth = ConformanceFixtures.nullableString(input, "dateOfBirth");
        if (dateOfBirth != null) {
            builder.dateOfBirth(LocalDate.parse(dateOfBirth));
        }

        String firstRegistrationYearMonth =
                ConformanceFixtures.nullableString(input, "firstRegistrationYearMonth");
        if (firstRegistrationYearMonth != null) {
            builder.firstRegistrationYearMonth(
                    YearMonth.parse(firstRegistrationYearMonth));
        }

        String dateOfRegistration =
                ConformanceFixtures.nullableString(input, "dateOfRegistration");
        if (dateOfRegistration != null) {
            builder.dateOfRegistration(LocalDate.parse(dateOfRegistration));
        }
        return builder.build();
    }
}
