package io.github.wal_n.hkid.name;

import io.github.wal_n.hkid.card.Sex;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidNameUtilTest {
    @Test
    void seedUsesRequestedColumnOrder() throws Exception {
        InputStream inputStream = HkidNameUtilTest.class.getClassLoader()
                .getResourceAsStream("io/github/wal_n/hkid/data/chinese-name-seed.csv");
        assertNotNull(inputStream);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            assertEquals(
                    "commercialCode,character,romanisation,commonSurname,sexAssociation,weight",
                    reader.readLine());
        }
    }

    @Test
    void seedDataSeparatesCommonSurnamesAndGivenNames() {
        List<ChineseNameEntry> entries = HkidNameUtil.getDefaultEntries();

        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(ChineseNameEntry::isCommonSurname));
        assertTrue(entries.stream().anyMatch(entry -> !entry.isCommonSurname()));
        assertTrue(entries.stream().filter(ChineseNameEntry::isCommonSurname)
                .allMatch(entry -> entry.getSupportedSexes().equals(
                        EnumSet.allOf(Sex.class))));
        assertTrue(entries.stream().anyMatch(
                entry -> !entry.isCommonSurname()
                        && entry.getSupportedSexes().equals(EnumSet.allOf(Sex.class))));
        for (Sex sex : Sex.values()) {
            assertTrue(entries.stream().anyMatch(
                    entry -> !entry.isCommonSurname()
                            && entry.getSupportedSexes().equals(EnumSet.of(sex))));
        }
        assertTrue(entries.stream().allMatch(
                entry -> entry.getRomanisation().matches("[A-Z][a-z]*")));
    }

    @Test
    void invalidSeedBooleanReportsPhysicalLineNumber() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> HkidNameUtil.loadEntries(
                        "io/github/wal_n/hkid/name/invalid-boolean-name-seed.csv"));

        assertNotNull(exception.getCause());
        assertEquals(
                "Invalid boolean at line 7: not-a-boolean",
                exception.getCause().getMessage());
    }

    @Test
    void invalidSeedSexAssociationReportsPhysicalLineNumber() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> HkidNameUtil.loadEntries(
                        "io/github/wal_n/hkid/name/invalid-sex-association-seed.csv"));

        assertNotNull(exception.getCause());
        assertEquals(
                "Invalid sex association at line 7: unknown",
                exception.getCause().getMessage());
    }

    @Test
    void emptySeedSexAssociationIsRejected() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> HkidNameUtil.loadEntries(
                        "io/github/wal_n/hkid/name/empty-sex-association-seed.csv"));

        assertNotNull(exception.getCause());
        assertEquals(
                "Invalid sex association at line 2: ",
                exception.getCause().getMessage());
    }

    @Test
    void generatedNamesSatisfySeedAndLengthInvariants() {
        for (int personalNameLength = 1; personalNameLength <= 5; personalNameLength++) {
            assertGeneratedNameMatchesSeed(
                    HkidNameUtil.generateRandomName(personalNameLength),
                    personalNameLength);
        }
    }

    @Test
    void sexSpecificGenerationUsesOnlyCompatibleGivenNameEntries() {
        for (Sex sex : Sex.values()) {
            for (int personalNameLength = 1; personalNameLength <= 5; personalNameLength++) {
                for (int seed = 0; seed < 50; seed++) {
                    GeneratedName name = HkidNameUtil.generateRandomName(
                            personalNameLength, sex, new Random(seed));

                    assertGeneratedNameMatchesSeed(name, personalNameLength);
                    assertGeneratedNameMatchesSex(name, sex);
                }
            }
        }
    }

    @Test
    void sexSpecificGenerationRejectsMissingDependencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HkidNameUtil.generateRandomName((Sex) null));
        assertThrows(
                IllegalArgumentException.class,
                () -> HkidNameUtil.generateRandomName(Sex.MALE, null));
    }

    @Test
    void sexSpecificGenerationIsDeterministicWithSeededRandom() {
        GeneratedName first = HkidNameUtil.generateRandomName(
                Sex.FEMALE, new Random(123456789L));
        GeneratedName second = HkidNameUtil.generateRandomName(
                Sex.FEMALE, new Random(123456789L));

        assertEquals(first.getChineseFullName(), second.getChineseFullName());
        assertEquals(first.getEnglishFullName(), second.getEnglishFullName());
        assertEquals(first.getCommercialCodes(), second.getCommercialCodes());
    }

    @Test
    void randomPersonalNameLengthUsesTenPercentBoundary() {
        for (int roll = 0; roll < 100; roll++) {
            assertEquals(roll < 10 ? 1 : 2, HkidNameUtil.defaultPersonalNameLengthForRoll(roll));
        }

        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.defaultPersonalNameLengthForRoll(-1));
        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.defaultPersonalNameLengthForRoll(100));
    }

    @Test
    void rejectsUnsupportedPersonalNameLength() {
        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.generateRandomName(0));
        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.generateRandomName(6));
    }

    @Test
    void exposesHongKongGovernmentRomanisation() {
        ChineseNameEntry entry = HkidNameUtil.getDefaultEntries().stream()
                .filter(candidate -> "陳".equals(candidate.getCharacter()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals("7115", entry.getCommercialCode());
        assertEquals("Chan", entry.getRomanisation());
        assertTrue(entry.isCommonSurname());
        assertEquals(EnumSet.allOf(Sex.class), entry.getSupportedSexes());
        assertThrows(UnsupportedOperationException.class, entry.getSupportedSexes()::clear);
    }

    @Test
    void surnameEntriesRejectSexSpecificClassification() {
        assertThrows(IllegalArgumentException.class, () -> new ChineseNameEntry(
                "0001", "測", "Test", true, EnumSet.of(Sex.MALE), 1));
    }

    private void assertGeneratedNameMatchesSeed(GeneratedName name, int personalNameLength) {
        assertNotNull(name.getChineseName());
        assertNotNull(name.getEnglishName());
        assertEquals(personalNameLength, name.getChineseName().getPersonalName().length());
        assertEquals(personalNameLength + 1, name.getCommercialCodes().size());

        Map<String, ChineseNameEntry> entriesByCharacter = new HashMap<>();
        for (ChineseNameEntry entry : HkidNameUtil.getDefaultEntries()) {
            entriesByCharacter.put(entry.getCharacter(), entry);
        }

        String fullName = name.getChineseFullName();
        for (int i = 0; i < fullName.length(); i++) {
            String character = String.valueOf(fullName.charAt(i));
            ChineseNameEntry entry = entriesByCharacter.get(character);
            assertNotNull(entry);
            assertEquals(entry.getCommercialCode(), name.getCommercialCodes().get(i));
            assertEquals(i == 0, entry.isCommonSurname());
        }

        ChineseNameEntry surname = entriesByCharacter.get(name.getChineseName().getSurname());
        assertTrue(surname.isCommonSurname());
        assertEquals(surname.getRomanisation(), name.getEnglishName().getSurname());
        assertFalse(name.getEnglishName().getPersonalName().isEmpty());
    }

    private void assertGeneratedNameMatchesSex(GeneratedName name, Sex sex) {
        Map<String, ChineseNameEntry> entriesByCharacter = new HashMap<>();
        for (ChineseNameEntry entry : HkidNameUtil.getDefaultEntries()) {
            entriesByCharacter.put(entry.getCharacter(), entry);
        }

        String personalName = name.getChineseName().getPersonalName();
        for (int i = 0; i < personalName.length(); i++) {
            ChineseNameEntry entry = entriesByCharacter.get(
                    String.valueOf(personalName.charAt(i)));
            assertNotNull(entry);
            assertTrue(entry.isCompatibleWith(sex));
        }
    }
}
