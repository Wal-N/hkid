package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonObject;
import io.github.wal_n.hkid.name.ChineseCommercialCode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HkidCommercialCodeConformanceTest {
    private static final String FIXTURE = "hkid-names.json";

    @TestFactory
    Stream<DynamicTest> preservesSharedCommercialCodeValuesAndDisplay() {
        return ConformanceFixtures.cases(FIXTURE, "commercialCodeCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> {
                            ChineseCommercialCode code = ConformanceFixtures.commercialCode(testCase.get("input"));
                            JsonObject expected = testCase.getAsJsonObject("expect");
                            assertAll(
                                    () -> assertEquals(expected.get("code").getAsString(), code.getCode()),
                                    () -> assertEquals(
                                            ConformanceFixtures.nullableString(expected, "extensionNumber"),
                                            code.getExtensionNumber().orElse(null)),
                                    () -> assertEquals(expected.get("display").getAsString(), code.toString()));
                        }));
    }

    @TestFactory
    Stream<DynamicTest> rejectsSharedInvalidCommercialCodeCases() {
        return ConformanceFixtures.cases(FIXTURE, "invalidCommercialCodeCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> assertThrows(
                                IllegalArgumentException.class,
                                () -> ConformanceFixtures.commercialCode(testCase.get("input")))));
    }

    @TestFactory
    Stream<DynamicTest> comparesSharedCommercialCodeValues() {
        return ConformanceFixtures.cases(FIXTURE, "commercialCodeComparisonCases")
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.get("id").getAsString(),
                        () -> {
                            ChineseCommercialCode left = ConformanceFixtures.commercialCode(testCase.get("left"));
                            ChineseCommercialCode right = ConformanceFixtures.commercialCode(testCase.get("right"));
                            boolean expected = testCase.get("expectSameValue").getAsBoolean();
                            assertAll(
                                    () -> assertEquals(expected, left.equals(right)),
                                    () -> assertEquals(expected, right.equals(left)));
                        }));
    }
}
