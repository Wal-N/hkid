package io.github.wal_n.hkid.name;

import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.Sex;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    void nameAssertionsHandleSupplementarySurnameAndPersonalName() {
        ChineseNameEntry surname = new ChineseNameEntry(
                "0001", "\uD840\uDC00", "Wan", true, EnumSet.allOf(Sex.class), 1);
        ChineseNameEntry supplementaryGivenName = new ChineseNameEntry(
                "0002", "\uD840\uDC01", "Yau", false, EnumSet.allOf(Sex.class), 1);
        ChineseNameEntry bmpGivenName = new ChineseNameEntry(
                "0003", "明", "Ming", false, EnumSet.allOf(Sex.class), 1);
        List<ChineseNameEntry> entries = Arrays.asList(surname, supplementaryGivenName, bmpGivenName);
        GeneratedName name = new GeneratedName(
                new ChineseName(surname.getCharacter(),
                        supplementaryGivenName.getCharacter() + bmpGivenName.getCharacter(),
                        Arrays.asList(surname.getCommercialCode(),
                                supplementaryGivenName.getCommercialCode(), bmpGivenName.getCommercialCode())),
                new EnglishName(surname.getRomanisation(), "Yau Ming"));

        assertGeneratedNameMatchesSeed(name, 2, entries);
        for (Sex sex : Sex.values()) {
            assertGeneratedNameMatchesSex(name, sex, entries);
        }
    }

    @Test
    void sexSpecificGenerationUsesOnlyCompatibleGivenNameEntries() {
        for (Sex sex : Sex.values()) {
            for (int personalNameLength = 1; personalNameLength <= 5; personalNameLength++) {
                for (int seed = 0; seed < 50; seed++) {
                    GeneratedName name = HkidNameUtil.generateRandomName(
                            new Random(seed), personalNameLength, sex);

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
                () -> HkidNameUtil.generateRandomName(null, Sex.MALE));
    }

    @Test
    void sexSpecificGenerationIsDeterministicWithSeededRandom() {
        GeneratedName first = HkidNameUtil.generateRandomName(
                new Random(123456789L), Sex.FEMALE);
        GeneratedName second = HkidNameUtil.generateRandomName(
                new Random(123456789L), Sex.FEMALE);

        assertEquals(first.getChineseNameString(), second.getChineseNameString());
        assertEquals(first.getEnglishNameString(), second.getEnglishNameString());
        assertEquals(first.getChineseCommercialCodes(), second.getChineseCommercialCodes());
    }

    @Test
    void generatedNamesAndCardsExposeTheSameNameValuesAndText() {
        GeneratedName generated = HkidNameUtil.generateRandomName(new Random(1234L), 2, Sex.FEMALE);
        HkidCard card = HkidCard.builder()
                .chineseName(generated.getChineseName())
                .englishName(generated.getEnglishName())
                .build();

        ChineseName chineseName = card.getChineseName();
        EnglishName englishName = card.getEnglishName();
        assertEquals(generated.getChineseName(), chineseName);
        assertEquals(generated.getEnglishName(), englishName);
        assertEquals(generated.getChineseCommercialCodes(), card.getChineseCommercialCodes());
        assertEquals(generated.getChineseNameString(), card.getChineseNameString());
        assertEquals(generated.getEnglishNameString(), card.getEnglishNameString());
        assertEquals(chineseName.getFullNameString(), card.getChineseNameString());
        assertEquals(englishName.getFullNameString(), card.getEnglishNameString());
        assertEquals(chineseName.getFullNameString(), chineseName.toString());
        assertEquals(englishName.getFullNameString(), englishName.toString());
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

        assertEquals(new ChineseCommercialCode("7115"), entry.getCommercialCode());
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
        assertGeneratedNameMatchesSeed(name, personalNameLength, HkidNameUtil.getDefaultEntries());
    }

    private void assertGeneratedNameMatchesSeed(
            GeneratedName name, int personalNameLength, List<ChineseNameEntry> entries) {
        assertNotNull(name.getChineseName());
        assertNotNull(name.getEnglishName());
        String personalName = name.getChineseName().getPersonalName();
        // A supplementary Han character (e.g. U+20000) uses two UTF-16 units but is one code point.
        // Count code points; length() is only the UTF-16 end offset for the whole string.
        assertEquals(personalNameLength, personalName.codePointCount(0, personalName.length()));
        assertEquals(personalNameLength + 1, name.getChineseCommercialCodes().size());

        Map<String, ChineseNameEntry> entriesByCharacter = new HashMap<>();
        for (ChineseNameEntry entry : entries) {
            entriesByCharacter.put(entry.getCharacter(), entry);
        }

        // Index by complete Han characters so each index matches one commercial code.
        // charAt() would split supplementary characters into surrogate halves.
        int[] codePoints = name.getChineseNameString().codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            String character = new String(Character.toChars(codePoints[i]));
            ChineseNameEntry entry = entriesByCharacter.get(character);
            assertNotNull(entry);
            assertEquals(entry.getCommercialCode(), name.getChineseCommercialCodes().get(i));
            assertEquals(i == 0, entry.isCommonSurname());
        }

        ChineseNameEntry surname = entriesByCharacter.get(name.getChineseName().getSurname());
        assertTrue(surname.isCommonSurname());
        assertEquals(surname.getRomanisation(), name.getEnglishName().getSurname());
        assertFalse(name.getEnglishName().getPersonalName().isEmpty());
    }

    private void assertGeneratedNameMatchesSex(GeneratedName name, Sex sex) {
        assertGeneratedNameMatchesSex(name, sex, HkidNameUtil.getDefaultEntries());
    }

    private void assertGeneratedNameMatchesSex(
            GeneratedName name, Sex sex, List<ChineseNameEntry> entries) {
        Map<String, ChineseNameEntry> entriesByCharacter = new HashMap<>();
        for (ChineseNameEntry entry : entries) {
            entriesByCharacter.put(entry.getCharacter(), entry);
        }

        String personalName = name.getChineseName().getPersonalName();
        // Reconstruct each complete character for seed lookup, including any surrogate pair.
        personalName.codePoints().forEach(codePoint -> {
            ChineseNameEntry entry = entriesByCharacter.get(
                    new String(Character.toChars(codePoint)));
            assertNotNull(entry);
            assertTrue(entry.isCompatibleWith(sex));
        });
    }
}
