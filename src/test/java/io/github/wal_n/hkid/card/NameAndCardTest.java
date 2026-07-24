package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.EnglishName;
import io.github.wal_n.hkid.name.EnglishNameUtil;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameAndCardTest {
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 21);

    @Test
    void cardAndNestedModelsAreImmutable() {
        assertImmutable(HkidCard.class);
        assertImmutable(ChineseName.class);
        assertImmutable(EnglishName.class);
        assertImmutable(HkidSymbols.class);
    }

    @Test
    void chineseNameValidatesAllPartsAtomically() {
        ChineseName name = new ChineseName(
                "陳",
                "大文",
                Arrays.asList("1234", "5678", "9999"));

        assertEquals("陳大文", name.getFullName());
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("陳", "大文一二三四", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("Chan", "大文", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("陳", "大文", Arrays.asList("1234", "5678")));
        assertThrows(UnsupportedOperationException.class,
                () -> name.getCommercialCodes().add("0000"));
    }

    @Test
    void englishNameValidatesNameParts() {
        EnglishName name = new EnglishName("Chan", "Tai Man");

        assertEquals("Chan, Tai Man", name.getFullName());
        assertTrue(EnglishNameUtil.isValidNamePart("Anne-Marie"));
        assertTrue(EnglishNameUtil.isValidNamePart("O'Connor"));
        assertFalse(EnglishNameUtil.isValidNamePart("A "));
        assertFalse(EnglishNameUtil.isValidNamePart("Chan---"));
        assertThrows(IllegalArgumentException.class, () -> new EnglishName("123", "Tai Man"));
        assertThrows(IllegalArgumentException.class, () -> new EnglishName("Chan---", "Tai Man"));
    }

    @Test
    void emptyCardUsesNonNullEmptyNames() {
        HkidCard card = HkidCard.builder().build();

        assertNotNull(card.getChineseNameInfo());
        assertNotNull(card.getEnglishNameInfo());
        assertEquals("", card.getChineseSurname());
        assertEquals("", card.getChinesePersonalName());
        assertEquals("", card.getChineseName());
        assertEquals("", card.getEnglishSurname());
        assertEquals("", card.getEnglishPersonalName());
        assertEquals("", card.getEnglishName());
        assertTrue(card.getChineseCommercialCodes().isEmpty());
    }

    @Test
    void builderNormalizesNullNamesAndNameParts() {
        HkidCard card = HkidCard.builder()
                .chineseName(null)
                .englishName(null)
                .chineseSurname(null)
                .chinesePersonalName(null)
                .englishSurname(null)
                .englishPersonalName(null)
                .build();

        assertEquals("", card.getChineseName());
        assertEquals("", card.getEnglishName());
    }

    @Test
    void builderValidatesNamesWhenCreatingTheImmutableCard() {
        HkidCard.Builder builder = HkidCard.builder()
                .chineseSurname("ABC")
                .englishSurname("123");

        assertThrows(IllegalArgumentException.class, builder::build);
        HkidCard validCard = builder
                .chineseSurname("陳")
                .englishSurname("Chan")
                .build();
        assertEquals("陳", validCard.getChineseName());
        assertEquals("Chan", validCard.getEnglishName());
    }

    @Test
    void cardReadsSymbolCodes() {
        HkidSymbols symbols = HkidSymbols.of(
                HkidSymbol.ADULT_RE_ENTRY_PERMIT,
                HkidSymbol.RIGHT_OF_ABODE,
                HkidSymbol.BORN_IN_HONG_KONG);
        HkidCard card = HkidCard.builder().symbols(symbols).build();

        assertEquals(symbols, card.getSymbols());
        assertEquals("***AZ", card.getSymbolCodes());
    }

    @Test
    void cardValidatesDateOrderAtBuildTime() {
        HkidCard card = HkidCard.builder()
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .firstRegistrationYearMonth(YearMonth.of(2001, 6))
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .build();

        assertEquals("06-01", card.getFirstRegistrationYearMonthStr());
        assertThrows(IllegalArgumentException.class, () -> HkidCard.builder()
                .dateOfRegistration(HkidCard.CURRENT_SMART_HKID_START_DATE.minusDays(1))
                .build());
        assertThrows(IllegalArgumentException.class, () -> card.toBuilder()
                .firstRegistrationYearMonth(YearMonth.of(1989, 12))
                .build());
        assertThrows(IllegalArgumentException.class, () -> card.toBuilder()
                .firstRegistrationYearMonth(YearMonth.of(2020, 7))
                .build());
        assertThrows(IllegalArgumentException.class, () -> card.toBuilder()
                .dateOfBirth(LocalDate.of(2002, 1, 1))
                .build());
        assertThrows(IllegalArgumentException.class, () -> card.toBuilder()
                .firstRegistrationYearMonth(YearMonth.of(2020, 6))
                .dateOfRegistration(LocalDate.of(2020, 5, 31))
                .build());
    }

    @Test
    void timeDependentChecksUseAnExplicitReferenceDate() {
        HkidCard futureCard = HkidCard.builder()
                .dateOfBirth(REFERENCE_DATE.plusDays(1))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> futureCard.getAge(REFERENCE_DATE));
        assertThrows(IllegalArgumentException.class,
                () -> futureCard.validateAsOf(REFERENCE_DATE));
        assertThrows(IllegalArgumentException.class,
                () -> futureCard.getAge(null));

        HkidCard historicalCard = HkidCard.builder()
                .dateOfBirth(LocalDate.of(1990, 7, 22))
                .build();
        assertEquals(Integer.valueOf(35), historicalCard.getAge(REFERENCE_DATE).orElse(null));
        assertEquals(Integer.valueOf(36),
                historicalCard.getAge(REFERENCE_DATE.plusDays(1)).orElse(null));
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
    void cardExposesSexMarkersAndPrintedValueSeparately() {
        HkidCard femaleCard = HkidCard.builder().sex(Sex.FEMALE).build();

        assertEquals("女", femaleCard.getSexChiMarker());
        assertEquals("F", femaleCard.getSexEngMarker());
        assertEquals("女 F", femaleCard.getSexPrintedValue());

        HkidCard maleCard = femaleCard.toBuilder().sexEngMarker("m").build();
        assertEquals(Sex.MALE, maleCard.getSex());
        assertEquals(Sex.FEMALE, femaleCard.getSex());
    }

    @Test
    void cardValidatesEligibilitySymbolAgainstAgeAtRegistration() {
        assertThrows(IllegalArgumentException.class, () -> HkidCard.builder()
                .dateOfBirth(LocalDate.of(2008, 1, 1))
                .symbolCodes("***AZ")
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .build());

        assertThrows(IllegalArgumentException.class, () -> HkidCard.builder()
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .symbolCodes("*AZ")
                .build());
    }

    @Test
    void historicalJuvenileCardRemainsValidAfterHolderTurnsEighteen() {
        HkidCard card = HkidCard.builder()
                .dateOfBirth(LocalDate.of(2008, 1, 1))
                .symbolCodes("*AZ")
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .build();

        card.validateAsOf(LocalDate.of(2025, 12, 31));
        card.validateAsOf(LocalDate.of(2026, 1, 1));
    }

    private static void assertImmutable(Class<?> type) {
        assertTrue(Modifier.isFinal(type.getModifiers()), type.getSimpleName());
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(Modifier.isFinal(field.getModifiers()),
                        type.getSimpleName() + "." + field.getName());
            }
        }
    }
}
