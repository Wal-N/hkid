package hkid;

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
                .getResourceAsStream("hkid/chinese-name-seed.csv");
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
        GeneratedName name = HkidNameUtil.genRandomName(1);

        assertGeneratedNameMatchesSeed(name, 1);
    }

    @Test
    void generatesTwoCharacterPersonalName() {
        GeneratedName name = HkidNameUtil.genRandomName(2);

        assertGeneratedNameMatchesSeed(name, 2);
    }

    @Test
    void randomPersonalNameLengthUsesTenPercentBoundary() {
        for (int roll = 0; roll < 100; roll++) {
            assertEquals(roll < 10 ? 1 : 2, HkidNameUtil.personalNameLengthForRoll(roll));
        }

        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.personalNameLengthForRoll(-1));
        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.personalNameLengthForRoll(100));
    }

    @Test
    void rejectsUnsupportedPersonalNameLength() {
        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.genRandomName(0));
        assertThrows(IllegalArgumentException.class, () -> HkidNameUtil.genRandomName(3));
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
        assertNotNull(name.getChiName());
        assertNotNull(name.getEngName());
        assertEquals(personalNameLength, name.getChiName().getPersonalName().length());
        assertEquals(personalNameLength + 1, name.getCommercialCodes().size());
        assertEquals(personalNameLength + 1, name.getRomanisationSyllables().size());

        Map<String, ChineseNameEntry> entriesByCharacter = new HashMap<>();
        for (ChineseNameEntry entry : HkidNameUtil.getDefaultEntries()) {
            entriesByCharacter.put(entry.getCharacter(), entry);
        }

        String fullName = name.getChiFullName();
        for (int i = 0; i < fullName.length(); i++) {
            String character = String.valueOf(fullName.charAt(i));
            ChineseNameEntry entry = entriesByCharacter.get(character);
            assertNotNull(entry);
            assertEquals(entry.getCommercialCode(), name.getCommercialCodes().get(i));
            assertEquals(entry.getRomanisation(), name.getRomanisationSyllables().get(i));
            assertEquals(i == 0, entry.isCommonSurname());
        }

        ChineseNameEntry surname = entriesByCharacter.get(name.getChiName().getSurname());
        assertTrue(surname.isCommonSurname());
        assertEquals(surname.getRomanisation(), name.getEngName().getSurname());
        assertEquals(String.join(" ", name.getRomanisationSyllables()), name.getRomanisation());
        assertEquals(
                String.join(" ", name.getRomanisationSyllables().subList(1, name.getRomanisationSyllables().size())),
                name.getEngName().getPersonalName());
        assertFalse(name.getEngName().getPersonalName().isEmpty());
    }
}
