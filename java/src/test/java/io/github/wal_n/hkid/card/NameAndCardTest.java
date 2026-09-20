package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseCommercialCode;
import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.ChineseNameUtil;
import io.github.wal_n.hkid.name.EnglishName;
import io.github.wal_n.hkid.name.EnglishNameUtil;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        assertImmutable(ChineseCommercialCode.class);
        assertImmutable(EnglishName.class);
        assertImmutable(HkidSymbols.class);
    }

    @Test
    void chineseNameValidatesAllPartsAtomically() {
        ChineseName name = new ChineseName(
                "陳",
                "大文",
                commercialCodes("1234", "5678", "9999"));

        assertEquals("陳大文", name.getFullName());
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("陳", "大文一二三四", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("Chan", "大文", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("陳", "大文", commercialCodes("1234", "5678")));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("", "", commercialCodes("1234")));
        assertThrows(IllegalArgumentException.class,
                () -> new ChineseName("陳", "大文", Arrays.asList(
                        new ChineseCommercialCode("1234"), new ChineseCommercialCode("5678"), null)));
        assertThrows(UnsupportedOperationException.class,
                () -> name.getCommercialCodes().add(new ChineseCommercialCode("0000")));
    }

    @Test
    void reportsWhetherChineseNamesAreValid() {
        assertTrue(ChineseNameUtil.isValid("陳", "大文"));
        assertTrue(ChineseNameUtil.isValid(null, null));
        assertFalse(ChineseNameUtil.isValid("Chan", "大文"));
        assertFalse(ChineseNameUtil.isValid("陳", "大文一二三四"));
        assertTrue(ChineseNameUtil.isValid(
                "陳", "大文", commercialCodes("1234", "5678", "9999")));
        assertTrue(ChineseNameUtil.isValid("陳", "大文", null));
        assertFalse(ChineseNameUtil.isValid(
                "陳", "大文", commercialCodes("1234", "5678")));
        assertFalse(ChineseNameUtil.isValid(
                "陳", "大文", Arrays.asList(
                        new ChineseCommercialCode("1234"), new ChineseCommercialCode("5678"), null)));
    }

    @Test
    void namesAndCardsPreserveCodeOrderAndCopyMutableLists() {
        List<ChineseCommercialCode> codes = new ArrayList<>(commercialCodes("0001", "0002", "0001"));
        ChineseName name = new ChineseName("陳", "大文", codes);
        HkidCard card = HkidCard.builder()
                .chineseSurname("陳")
                .chinesePersonalName("大文")
                .chineseCommercialCodes(codes)
                .build();
        codes.clear();

        assertEquals(commercialCodes("0001", "0002", "0001"), name.getCommercialCodes());
        assertEquals(name, card.getChineseNameInfo());
        assertEquals(card, card.toBuilder().build());
        assertEquals(card.hashCode(), card.toBuilder().build().hashCode());
        assertEquals(card, HkidCard.builder().chineseName(name).build());
        assertThrows(UnsupportedOperationException.class, card.getChineseCommercialCodes()::clear);
        assertThrows(IllegalArgumentException.class,
                () -> card.toBuilder().chinesePersonalName("大").build());
        assertTrue(card.toBuilder().chineseCommercialCodes(null).build().getChineseCommercialCodes().isEmpty());
    }

    @Test
    void commercialCodeListsAreOptionalAndLimitedToSixEntries() {
        ChineseCommercialCode code = new ChineseCommercialCode("0001");

        assertTrue(ChineseNameUtil.isValidChineseCommercialCodes(null));
        assertTrue(ChineseNameUtil.isValidChineseCommercialCodes(Collections.emptyList()));
        assertTrue(ChineseNameUtil.isValidChineseCommercialCodes(Collections.nCopies(6, code)));
        assertFalse(ChineseNameUtil.isValidChineseCommercialCodes(Collections.nCopies(7, code)));
        assertFalse(ChineseNameUtil.isValidChineseCommercialCodes(Collections.singletonList(null)));
        assertTrue(new ChineseName("陳", "大文", null).getCommercialCodes().isEmpty());
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
    void reportsWhetherEnglishNamesAreValid() {
        assertTrue(EnglishNameUtil.isValid("Chan", "Tai Man"));
        assertTrue(EnglishNameUtil.isValid("O'Connor", "Anne-Marie"));
        assertFalse(EnglishNameUtil.isValid(null, null));
        assertFalse(EnglishNameUtil.isValid("", "Tai Man"));
        assertTrue(EnglishNameUtil.isValid("Chan", ""));
        assertFalse(EnglishNameUtil.isValid("123", "Tai Man"));
        assertFalse(EnglishNameUtil.isValid("Chan---", "Tai Man"));
    }

    @Test
    void rejectsOverlongEnglishNamesBeforeMatchingNameParts() {
        String longName = String.join(" ", Collections.nCopies(10000, "A"));

        assertFalse(EnglishNameUtil.isValid(longName, "Tai Man"));
        assertFalse(EnglishNameUtil.isValid("Chan", longName));
    }

    @Test
    void englishNamePartsHandleLongInputsWithoutRecursion() {
        for (String separator : Arrays.asList(" ", ".", "'", "-")) {
            String longName = String.join(separator, Collections.nCopies(10000, "A"));

            assertTrue(EnglishNameUtil.isValidNamePart(longName));
            assertFalse(EnglishNameUtil.isValidNamePart(longName + separator));
            assertFalse(EnglishNameUtil.isValidNamePart(longName + separator + separator + "A"));
            assertFalse(EnglishNameUtil.isValidNamePart(longName + "1"));
        }
    }

    @Test
    void englishNamePartsRequireAsciiLettersBetweenSingleSeparators() {
        for (String valid : Arrays.asList("A", "aZ", "St.John", "Anne-Marie O'Connor")) {
            assertTrue(EnglishNameUtil.isValidNamePart(valid), valid);
        }
        for (String invalid : Arrays.asList(null, "", " ", ".A", "'A", "-A", " A",
                "A.", "A'", "A-", "A ", "A.-B", "A  B", "A\tB", "A\nB", "A,B", "Jos\u00e9")) {
            assertFalse(EnglishNameUtil.isValidNamePart(invalid), invalid);
        }
    }

    @Test
    void englishNameConstructorRejectsLongSeparatedParts() {
        String longName = String.join(" ", Collections.nCopies(10000, "A"));

        assertThrows(IllegalArgumentException.class, () -> new EnglishName(longName, "Tai Man"));
        assertThrows(IllegalArgumentException.class, () -> new EnglishName("Chan", longName));
    }

    @Test
    void cardBuilderRejectsLongSeparatedEnglishNameParts() {
        String longName = String.join(" ", Collections.nCopies(10000, "A"));

        assertThrows(IllegalArgumentException.class,
                () -> HkidCard.builder().englishSurname(longName).build());
        assertThrows(IllegalArgumentException.class,
                () -> HkidCard.builder().englishPersonalName(longName).build());
    }

    @Test
    void englishNameLengthIncludesCommaAndSpace() {
        for (int length : Arrays.asList(39, 40)) {
            String personalName = String.join("", Collections.nCopies(length - 6, "A"));

            assertTrue(EnglishNameUtil.isValid("Chan", personalName));
            assertEquals(length, new EnglishName("Chan", personalName).getFullName().length());
            assertEquals(length, HkidCard.builder()
                    .englishSurname("Chan").englishPersonalName(personalName).build().getEnglishName().length());
        }

        String personalName = String.join("", Collections.nCopies(35, "A"));
        assertFalse(EnglishNameUtil.isValid("Chan", personalName));
        assertThrows(IllegalArgumentException.class, () -> new EnglishName("Chan", personalName));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCard.builder().englishSurname("Chan").englishPersonalName(personalName).build());
    }

    @Test
    void englishNameLengthOmitsSeparatorForEmptyParts() {
        String atLimit = String.join("", Collections.nCopies(40, "A"));
        String overLimit = atLimit + "A";

        for (String emptyPart : Arrays.asList(null, "")) {
            assertEquals("", new EnglishName(emptyPart, emptyPart).getFullName());
            assertTrue(EnglishNameUtil.isValid(atLimit, emptyPart));
            assertFalse(EnglishNameUtil.isValid(overLimit, emptyPart));
            assertFalse(EnglishNameUtil.isValid(emptyPart, atLimit));
            assertEquals(atLimit, new EnglishName(atLimit, emptyPart).getFullName());
            assertEquals(atLimit, new EnglishName(emptyPart, atLimit).getFullName());
            assertThrows(IllegalArgumentException.class, () -> new EnglishName(overLimit, emptyPart));
            assertThrows(IllegalArgumentException.class, () -> new EnglishName(emptyPart, overLimit));
            assertEquals(atLimit, HkidCard.builder()
                    .englishSurname(atLimit).englishPersonalName(emptyPart).build().getEnglishName());
            assertEquals(atLimit, HkidCard.builder()
                    .englishSurname(emptyPart).englishPersonalName(atLimit).build().getEnglishName());
            assertThrows(IllegalArgumentException.class,
                    () -> HkidCard.builder().englishSurname(overLimit).englishPersonalName(emptyPart).build());
            assertThrows(IllegalArgumentException.class,
                    () -> HkidCard.builder().englishSurname(emptyPart).englishPersonalName(overLimit).build());
        }
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

    private static List<ChineseCommercialCode> commercialCodes(String... codes) {
        return Arrays.stream(codes).map(ChineseCommercialCode::new).collect(Collectors.toList());
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
