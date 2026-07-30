package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonObject;
import io.github.wal_n.hkid.card.HkidSymbols;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HkidSymbolsConformanceTest {
    private static final String FIXTURE = "hkid-symbols.json";

    @TestFactory
    Stream<DynamicTest> parsesAndCanonicalizesSharedCases() {
        return ConformanceFixtures.cases(FIXTURE, "parseCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertEquals(
                                testCase.get("expectCanonical").getAsString(),
                                HkidSymbols.parse(testCase.get("input").getAsString()).toString())));
    }

    @TestFactory
    Stream<DynamicTest> rejectsSharedInvalidCases() {
        return ConformanceFixtures.cases(FIXTURE, "invalidParseCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertThrows(
                                IllegalArgumentException.class,
                                () -> HkidSymbols.parse(testCase.get("input").getAsString()))));
    }

    @TestFactory
    Stream<DynamicTest> validatesSharedAgeCases() {
        return ConformanceFixtures.cases(FIXTURE, "ageValidationCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertAgeValidation(testCase)));
    }

    private static void assertAgeValidation(JsonObject testCase) {
        HkidSymbols symbols = HkidSymbols.parse(testCase.get("symbols").getAsString());
        LocalDate dateOfBirth = LocalDate.parse(testCase.get("dateOfBirth").getAsString());
        LocalDate referenceDate = LocalDate.parse(testCase.get("referenceDate").getAsString());

        if (testCase.get("expectValid").getAsBoolean()) {
            assertDoesNotThrow(() -> symbols.validateAge(dateOfBirth, referenceDate));
        } else {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> symbols.validateAge(dateOfBirth, referenceDate));
        }
    }
}
