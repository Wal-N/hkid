package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonObject;
import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.HkidSymbols;
import io.github.wal_n.hkid.card.Sex;
import io.github.wal_n.hkid.number.HkidNumber;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                                () -> buildDatedCard(testCase))));
    }

    @TestFactory
    Stream<DynamicTest>validatesSharedAsOfCases() {
        return ConformanceFixtures.cases(FIXTURE, "asOfValidationCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertAsOfValidation(testCase)));
    }

    private static void assertCard(JsonObject testCase) {
        HkidCard card = buildCompleteCard(testCase.getAsJsonObject("input"));
        JsonObject expected = testCase.getAsJsonObject("expect");
        LocalDate referenceDate = LocalDate.parse(
                testCase.get("referenceDate").getAsString());

        assertAll(
                () -> assertEquals(
                        expected.get("hkidWithoutCheckDigit").getAsString(),
                        card.getHkidNumberStr()),
                () -> assertEquals(
                        expected.get("hkidComplete").getAsString(),
                        card.getHkidNumberStr(HkidNumber.Format.COMPLETE)),
                () -> assertEquals(
                        expected.get("hkidMasked").getAsString(),
                        card.getHkidNumberMaskedStr()),
                () -> assertEquals(
                        expected.get("chineseName").getAsString(),
                        card.getChineseName()),
                () -> assertEquals(
                        expected.get("englishName").getAsString(),
                        card.getEnglishName()),
                () -> assertEquals(
                        expected.get("sexPrintedValue").getAsString(),
                        card.getSexPrintedValue()),
                () -> assertEquals(
                        expected.get("dateOfBirth").getAsString(),
                        card.getDateOfBirthStr()),
                () -> assertEquals(
                        expected.get("symbols").getAsString(),
                        card.getSymbolCodes()),
                () -> assertEquals(
                        expected.get("firstRegistrationYearMonth").getAsString(),
                        card.getFirstRegistrationYearMonthStr()),
                () -> assertEquals(
                        expected.get("dateOfRegistration").getAsString(),
                        card.getDateOfRegistrationStr()),
                () -> assertEquals(
                        Integer.valueOf(expected.get("age").getAsInt()),
                        card.getAge(referenceDate).orElse(null)),
                () -> assertDoesNotThrow(() -> card.validateAsOf(referenceDate)));
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
        HkidCard card = buildDatedCard(testCase);
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

    private static HkidCard buildCompleteCard(JsonObject input) {
        return HkidCard.builder()
                .hkidNumber(new HkidNumber(input.get("hkidNumber").getAsString()))
                .chineseSurname(input.get("chineseSurname").getAsString())
                .chinesePersonalName(input.get("chinesePersonalName").getAsString())
                .chineseCommercialCodes(
                        ConformanceFixtures.strings(input, "chineseCommercialCodes"))
                .englishSurname(input.get("englishSurname").getAsString())
                .englishPersonalName(input.get("englishPersonalName").getAsString())
                .sex(Sex.fromEngMarker(input.get("sex").getAsString()))
                .dateOfBirth(LocalDate.parse(input.get("dateOfBirth").getAsString()))
                .symbolCodes(input.get("symbols").getAsString())
                .firstRegistrationYearMonth(
                        YearMonth.parse(input.get("firstRegistrationYearMonth").getAsString()))
                .dateOfRegistration(
                        LocalDate.parse(input.get("dateOfRegistration").getAsString()))
                .build();
    }

    private static HkidCard buildDatedCard(JsonObject input) {
        HkidCard.Builder builder = HkidCard.builder()
                .symbols(HkidSymbols.parse(input.get("symbols").getAsString()));

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
