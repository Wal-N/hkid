package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.EnglishName;
import io.github.wal_n.hkid.name.EnglishNameUtil;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameAndCardTest {
    @Test
    void chineseNameValidatesUpdatedSurnameAndPersonalNameTogether() {
        ChineseName name = new ChineseName();
        name.setSurname("陳");
        name.setPersonalName("大文");
        name.setCommercialCodes(Arrays.asList("1234", "5678", "9999"));

        assertEquals("陳大文", name.getFullName());
        assertThrows(IllegalArgumentException.class, () -> name.setPersonalName("大文一二三四"));
        assertThrows(IllegalArgumentException.class, () -> name.setSurname("Chan"));
        assertThrows(IllegalArgumentException.class, () -> name.setCommercialCodes(Arrays.asList("1234", "5678")));
    }

    @Test
    void englishNameValidatesNameParts() {
        EnglishName name = new EnglishName("Chan", "Tai Man");

        assertEquals("Chan, Tai Man", name.getFullName());
        assertTrue(EnglishNameUtil.isValidNamePart("Anne-Marie"));
        assertTrue(EnglishNameUtil.isValidNamePart("O'Connor"));
        assertFalse(EnglishNameUtil.isValidNamePart("A "));
        assertFalse(EnglishNameUtil.isValidNamePart("Chan---"));
        assertThrows(IllegalArgumentException.class, () -> name.setSurname("123"));
        assertThrows(IllegalArgumentException.class, () -> name.setSurname("Chan---"));
    }

    @Test
    void hkidReturnsEmptyCommercialCodeListWhenChineseNameIsMissing() {
        HkidCard hkid = new HkidCard();

        assertTrue(hkid.getChineseCommercialCodes().isEmpty());
    }

    @Test
    void namesDefaultAndNormalizeToEmptyStrings() {
        HkidCard hkid = new HkidCard();

        assertNotNull(hkid.getChineseNameInfo());
        assertNotNull(hkid.getEnglishNameInfo());
        assertEquals("", hkid.getChineseSurname());
        assertEquals("", hkid.getChinesePersonalName());
        assertEquals("", hkid.getChineseName());
        assertEquals("", hkid.getEnglishSurname());
        assertEquals("", hkid.getEnglishPersonalName());
        assertEquals("", hkid.getEnglishName());

        hkid.setChineseName(null);
        hkid.setEnglishName(null);
        hkid.setChineseSurname(null);
        hkid.setChinesePersonalName(null);
        hkid.setEnglishSurname(null);
        hkid.setEnglishPersonalName(null);

        assertNotNull(hkid.getChineseNameInfo());
        assertNotNull(hkid.getEnglishNameInfo());
        assertEquals("", hkid.getChineseSurname());
        assertEquals("", hkid.getChinesePersonalName());
        assertEquals("", hkid.getChineseName());
        assertEquals("", hkid.getEnglishSurname());
        assertEquals("", hkid.getEnglishPersonalName());
        assertEquals("", hkid.getEnglishName());
    }

    @Test
    void failedNameSettersLeaveEmptyNameObjectsUnchanged() {
        HkidCard hkid = new HkidCard();
        ChineseName chineseName = hkid.getChineseNameInfo();
        EnglishName englishName = hkid.getEnglishNameInfo();

        assertThrows(IllegalArgumentException.class, () -> hkid.setChineseSurname("ABC"));
        assertThrows(IllegalArgumentException.class, () -> hkid.setChinesePersonalName("ABC"));
        assertThrows(IllegalArgumentException.class,
                () -> hkid.setChineseCommercialCodes(Arrays.asList("123")));
        assertSame(chineseName, hkid.getChineseNameInfo());
        assertEquals("", hkid.getChineseName());
        assertTrue(hkid.getChineseCommercialCodes().isEmpty());

        assertThrows(IllegalArgumentException.class, () -> hkid.setEnglishSurname("123"));
        assertThrows(IllegalArgumentException.class, () -> hkid.setEnglishPersonalName("123"));
        assertSame(englishName, hkid.getEnglishNameInfo());
        assertEquals("", hkid.getEnglishName());
    }

    @Test
    void hkidReadsAndSetsSymbolCodes() {
        HkidCard hkid = new HkidCard();
        HkidSymbols symbols = HkidSymbols.of(
                HkidSymbol.ADULT_RE_ENTRY_PERMIT,
                HkidSymbol.RIGHT_OF_ABODE,
                HkidSymbol.BORN_IN_HONG_KONG);
        hkid.setSymbols(symbols);

        assertEquals(symbols, hkid.getSymbols());
        assertEquals("***AZ", hkid.getSymbolCodes());
    }

    @Test
    void hkidValidatesDateOrder() {
        HkidCard hkid = new HkidCard();
        hkid.setDateOfBirth(LocalDate.of(1990, 1, 1));
        hkid.setFirstRegistrationYearMonth(YearMonth.of(2001, 6));
        hkid.setDateOfRegistration(LocalDate.of(2020, 6, 1));

        assertEquals("06-01", hkid.getFirstRegistrationYearMonthStr());
        assertThrows(IllegalArgumentException.class, () -> hkid.setDateOfBirth(LocalDate.now().plusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> hkid.setDateOfRegistration(HkidCard.CURRENT_SMART_HKID_START_DATE.minusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> hkid.setFirstRegistrationYearMonth(YearMonth.of(1989, 12)));
        assertThrows(IllegalArgumentException.class,
                () -> hkid.setFirstRegistrationYearMonth(YearMonth.of(2020, 7)));
        assertThrows(IllegalArgumentException.class,
                () -> hkid.setDateOfBirth(LocalDate.of(2002, 1, 1)));

        hkid.setFirstRegistrationYearMonth(YearMonth.of(2020, 6));
        assertThrows(IllegalArgumentException.class,
                () -> hkid.setDateOfRegistration(LocalDate.of(2020, 5, 31)));
    }

    @Test
    void sexParsesCaseInsensitiveEnglishMarker() {
        assertEquals(Sex.MALE, Sex.fromEngMarker("m"));
        assertEquals("男", Sex.MALE.getChiMarker());
        assertEquals("M", Sex.MALE.getEngMarker());
        assertEquals("男 M", Sex.MALE.getPrintedValue());
        assertEquals("女 F", Sex.FEMALE.toString());
        assertThrows(IllegalArgumentException.class, () -> Sex.fromEngMarker("X"));
    }

    @Test
    void hkidExposesSexMarkersAndPrintedValueSeparately() {
        HkidCard hkid = new HkidCard();
        hkid.setSex(Sex.FEMALE);

        assertEquals("女", hkid.getSexChiMarker());
        assertEquals("F", hkid.getSexEngMarker());
        assertEquals("女 F", hkid.getSexPrintedValue());

        hkid.setSexEngMarker("m");
        assertEquals(Sex.MALE, hkid.getSex());
    }

    @Test
    void hkidValidatesEligibilitySymbolAgainstAgeAtRegistration() {
        HkidCard impossibleAdultCard = new HkidCard();
        impossibleAdultCard.setDateOfBirth(LocalDate.of(2008, 1, 1));
        impossibleAdultCard.setSymbolCodes("***AZ");

        assertThrows(IllegalArgumentException.class,
                () -> impossibleAdultCard.setDateOfRegistration(LocalDate.of(2020, 6, 1)));

        HkidCard impossibleAdultCardLoadedInAnotherOrder = new HkidCard();
        impossibleAdultCardLoadedInAnotherOrder.setDateOfRegistration(LocalDate.of(2020, 6, 1));
        impossibleAdultCardLoadedInAnotherOrder.setSymbolCodes("***AZ");

        assertThrows(IllegalArgumentException.class,
                () -> impossibleAdultCardLoadedInAnotherOrder.setDateOfBirth(LocalDate.of(2008, 1, 1)));

        HkidCard impossibleMinorCard = new HkidCard();
        impossibleMinorCard.setDateOfRegistration(LocalDate.of(2020, 6, 1));
        impossibleMinorCard.setDateOfBirth(LocalDate.of(1990, 1, 1));

        assertThrows(IllegalArgumentException.class, () -> impossibleMinorCard.setSymbolCodes("*AZ"));
    }

    @Test
    void hkidCanLoadHistoricalJuvenileCardAndValidateItAsOfAnotherDate() {
        HkidCard card = new HkidCard();
        card.setDateOfBirth(LocalDate.of(2008, 1, 1));
        card.setSymbolCodes("*AZ");
        card.setDateOfRegistration(LocalDate.of(2020, 6, 1));

        card.validateAsOf(LocalDate.of(2025, 12, 31));
        assertThrows(IllegalArgumentException.class,
                () -> card.validateAsOf(LocalDate.of(2026, 1, 1)));
    }

}
