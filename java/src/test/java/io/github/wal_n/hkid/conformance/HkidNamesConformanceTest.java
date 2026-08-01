package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonObject;
import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.EnglishName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HkidNamesConformanceTest {
    private static final String FIXTURE = "hkid-names.json";

    @TestFactory
    Stream<DynamicTest> constructsSharedChineseNameCases() {
        return ConformanceFixtures.cases(FIXTURE, "chineseNameCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> {
                            ChineseName name = buildChineseName(testCase);
                            assertAll(
                                    () -> assertEquals(
                                            testCase.get("expectFullName").getAsString(),
                                            name.getFullName()),
                                    () -> assertEquals(
                                            testCase.get("surname").getAsString(),
                                            name.getSurname()),
                                    () -> assertEquals(
                                            testCase.get("personalName").getAsString(),
                                            name.getPersonalName()),
                                    () -> assertEquals(
                                            ConformanceFixtures.strings(
                                                    testCase, "commercialCodes"),
                                            name.getCommercialCodes()));
                        }));
    }

    @TestFactory
    Stream<DynamicTest> rejectsSharedInvalidChineseNameCases() {
        return ConformanceFixtures.cases(FIXTURE, "invalidChineseNameCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertThrows(
                                IllegalArgumentException.class,
                                () -> buildChineseName(testCase))));
    }

    @TestFactory
    Stream<DynamicTest> constructsSharedEnglishNameCases() {
        return ConformanceFixtures.cases(FIXTURE, "englishNameCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> {
                            EnglishName name = buildEnglishName(testCase);
                            assertAll(
                                    () -> assertEquals(
                                            testCase.get("expectFullName").getAsString(),
                                            name.getFullName()),
                                    () -> assertEquals(
                                            testCase.get("surname").getAsString(),
                                            name.getSurname()),
                                    () -> assertEquals(
                                            testCase.get("personalName").getAsString(),
                                            name.getPersonalName()));
                        }));
    }

    @TestFactory
    Stream<DynamicTest> rejectsSharedInvalidEnglishNameCases() {
        return ConformanceFixtures.cases(FIXTURE, "invalidEnglishNameCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertThrows(
                                IllegalArgumentException.class,
                                () -> buildEnglishName(testCase))));
    }

    private static ChineseName buildChineseName(JsonObject testCase) {
        return new ChineseName(
                testCase.get("surname").getAsString(),
                testCase.get("personalName").getAsString(),
                ConformanceFixtures.strings(testCase, "commercialCodes"));
    }

    private static EnglishName buildEnglishName(JsonObject testCase) {
        return new EnglishName(
                testCase.get("surname").getAsString(),
                testCase.get("personalName").getAsString());
    }
}
