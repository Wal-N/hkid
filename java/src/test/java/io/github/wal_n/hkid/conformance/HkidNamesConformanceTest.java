package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.wal_n.hkid.name.ChineseCommercialCode;
import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.EnglishName;
import io.github.wal_n.hkid.name.EnglishNameUtil;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
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
                                            name.getFullNameString()),
                                    () -> assertEquals(
                                            testCase.get("surname").getAsString(),
                                            name.getSurname()),
                                    () -> assertEquals(
                                            testCase.get("personalName").getAsString(),
                                            name.getPersonalName()),
                                    () -> assertCommercialCodes(testCase, name.getCommercialCodes()));
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
    Stream<DynamicTest> validatesSharedEnglishNameCases() {
        return ConformanceFixtures.cases(FIXTURE, "englishNameValidationCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertEquals(
                                testCase.get("expectValid").getAsBoolean(),
                                EnglishNameUtil.isValid(
                                        ConformanceFixtures.nullableString(testCase, "surname"),
                                        ConformanceFixtures.nullableString(testCase, "personalName")))));
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
                                            name.getFullNameString()),
                                    () -> assertEquals(
                                            expectedNamePart(testCase, "surname", "expectSurname"),
                                            name.getSurname()),
                                    () -> assertEquals(
                                            expectedNamePart(testCase, "personalName", "expectPersonalName"),
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

    private static void assertCommercialCodes(
            JsonObject testCase, List<ChineseCommercialCode> actual) {
        JsonArray expected = testCase.getAsJsonArray(
                testCase.has("expectCommercialCodes") ? "expectCommercialCodes" : "commercialCodes");
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            JsonObject value = expected.get(i).getAsJsonObject();
            String code = value.get("code").getAsString();
            String extension = ConformanceFixtures.nullableString(value, "extensionNumber");
            String display = value.has("display") ? value.get("display").getAsString() : code;
            ChineseCommercialCode actualCode = actual.get(i);
            assertAll(
                    () -> assertEquals(code, actualCode.getCode()),
                    () -> assertEquals(extension, actualCode.getExtensionNumber().orElse(null)),
                    () -> assertEquals(display, actualCode.toString()));
        }
    }

    private static ChineseName buildChineseName(JsonObject testCase) {
        return new ChineseName(
                testCase.get("surname").getAsString(),
                testCase.get("personalName").getAsString(),
                ConformanceFixtures.commercialCodes(testCase, "commercialCodes"));
    }

    private static EnglishName buildEnglishName(JsonObject testCase) {
        return new EnglishName(
                ConformanceFixtures.requireNullableString(testCase, "surname"),
                ConformanceFixtures.requireNullableString(testCase, "personalName"));
    }

    private static String expectedNamePart(JsonObject testCase, String inputField, String expectedField) {
        return testCase.get(testCase.has(expectedField) ? expectedField : inputField).getAsString();
    }
}
