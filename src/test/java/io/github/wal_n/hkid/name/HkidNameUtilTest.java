package io.github.wal_n.hkid.name;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidNameUtilTest {
    @Test
    void seedUsesRequestedColumnOrder() throws Exception {
        InputStream inputStream = HkidNameUtilTest.class.getClassLoader()
                .getResourceAsStream("io/github/wal_n/hkid/name/chinese-name-seed.csv");
        assertNotNull(inputStream);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            assertEquals("commercialCode,character,romanisation,commonSurname,weight", reader.readLine());
        }
    }

    @Test
    void seedDataSeparatesCommonSurnamesAndGivenNames() {
        List<ChineseNameEntry> entries = HkidNameUtil.getDefaultEntries();

        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(ChineseNameEntry::isCommonSurname));
        assertTrue(entries.stream().anyMatch(entry -> !entry.isCommonSurname()));
        assertTrue(entries.stream().allMatch(
                entry -> entry.getRomanisation().matches("[A-Z][a-z]*")));
    }

    @Test
    void generatesOneCharacterPersonalName() {
        GeneratedName name = HkidNameUtil.generateRandomName(1);

        assertGeneratedNameMatchesSeed(name, 1);
    }

    @Test
    void generatesTwoCharacterPersonalName() {
        GeneratedName name = HkidNameUtil.generateRandomName(2);

        assertGeneratedNameMatchesSeed(name, 2);
    }

    @Test
    void generatesFiveCharacterPersonalName() {
        GeneratedName name = HkidNameUtil.generateRandomName(5);

        assertGeneratedNameMatchesSeed(name, 5);
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
}
