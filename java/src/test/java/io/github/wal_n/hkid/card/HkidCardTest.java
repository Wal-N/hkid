package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseNameEntry;
import io.github.wal_n.hkid.name.HkidNameUtil;
import io.github.wal_n.hkid.number.DefinedPrefix;
import io.github.wal_n.hkid.number.HkidNumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidCardTest {
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 21);

    @ParameterizedTest
    @CsvSource({"11, *AZ", "100, ***AZ"})
    void generatesCompleteCardAtAgeLimits(int age, String symbols) {
        HkidCard card = HkidCardUtil.generateRandomCard(
                REFERENCE_DATE, age, age, HkidSymbol.BORN_IN_HONG_KONG);

        assertGeneratedCard(card, REFERENCE_DATE);
        assertEquals(Integer.valueOf(age), card.getAge(REFERENCE_DATE).orElse(null));
        assertEquals(symbols, card.getSymbolsString());
    }

    @Test
    void generatesValidCardOnEleventhAnniversaryOfLeapDayBirth() {
        LocalDate referenceDate = LocalDate.of(2027, 3, 1);
        HkidCard card = HkidCardUtil.generateCompleteCard(
                referenceDate, null, LocalDate.of(2016, 2, 29), HkidSymbol.BORN_IN_HONG_KONG);

        assertEquals(referenceDate, card.getDateOfRegistration());
        assertEquals(YearMonth.of(2027, 3), card.getFirstRegistrationYearMonth());
        assertEquals(Integer.valueOf(11), card.getAge(referenceDate).orElse(null));
        assertEquals(HkidSymbols.parse("*AZ"), card.getSymbols());
        card.validateAsOf(referenceDate);
    }

    @Test
    void randomLeapDayCardChangesToAdultSymbolOnMarchFirst() {
        LocalDate dateOfBirth = LocalDate.of(2004, 2, 29);
        LocalDate dayBeforeAnniversary = LocalDate.of(2022, 2, 28);
        HkidCard juvenileCard = HkidCardUtil.generateCompleteCard(
                dayBeforeAnniversary, null, dateOfBirth, HkidSymbol.BORN_IN_HONG_KONG);

        assertEquals(dateOfBirth, juvenileCard.getDateOfBirth());
        assertEquals(HkidSymbols.parse("*AZ"), juvenileCard.getSymbols());
        assertEquals(Integer.valueOf(17),
                juvenileCard.getAge(dayBeforeAnniversary).orElse(null));

        LocalDate anniversary = LocalDate.of(2022, 3, 1);
        HkidCard adultCard = HkidCardUtil.generateCompleteCard(
                anniversary, null, dateOfBirth, HkidSymbol.BORN_IN_HONG_KONG);

        assertEquals(dateOfBirth, adultCard.getDateOfBirth());
        assertEquals(HkidSymbols.parse("***AZ"), adultCard.getSymbols());
        assertEquals(anniversary, adultCard.getDateOfRegistration());
        assertEquals(Integer.valueOf(18), adultCard.getAge(anniversary).orElse(null));
    }

    @Test
    void rejectsMissingGenerationArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard((LocalDate) null));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard((Sex) null));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, null));
    }

    @Test
    void rejectsReferenceDateBeforeCurrentSmartHkidStartDate() {
        LocalDate unsupportedDate = HkidCard.CURRENT_SMART_HKID_START_DATE.minusDays(1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(unsupportedDate));
        assertEquals("Reference date cannot be before " + HkidCard.CURRENT_SMART_HKID_START_DATE,
                error.getMessage());
    }

    @Test
    void acceptsCurrentSmartHkidStartDate() {
        HkidCard card = HkidCardUtil.generateRandomCard(HkidCard.CURRENT_SMART_HKID_START_DATE);

        assertEquals(HkidCard.CURRENT_SMART_HKID_START_DATE, card.getDateOfRegistration());
        card.validateAsOf(HkidCard.CURRENT_SMART_HKID_START_DATE);
    }

    @Test
    void generatesCompleteCardForCallerControlledReferenceDate() {
        assertGeneratedCard(HkidCardUtil.generateRandomCard(REFERENCE_DATE), REFERENCE_DATE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void defaultCardsUseCensusBirthplacesAndAgesElevenThroughEightyFive(boolean requestSex) {
        boolean sawHongKong = false;
        boolean sawOtherBirthplace = false;
        for (int i = 0; i < 256; i++) {
            HkidCard card = requestSex
                    ? HkidCardUtil.generateRandomCard(REFERENCE_DATE, Sex.FEMALE)
                    : HkidCardUtil.generateRandomCard(REFERENCE_DATE);
            int age = card.getAge(REFERENCE_DATE).get();
            assertTrue(age >= 11 && age <= 85, "Default age outside 11–85: " + age);
            assertEquals(1, card.getSymbols().getByCategory(HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH).size());
            boolean hongKong = card.getSymbols().contains(HkidSymbol.BORN_IN_HONG_KONG);
            sawHongKong |= hongKong;
            sawOtherBirthplace |= !hongKong;
        }
        assertTrue(sawHongKong, "Expected a Hong Kong birthplace");
        assertTrue(sawOtherBirthplace, "Expected a birthplace outside Hong Kong");
    }

    @Test
    void generatesCompleteCardWithRequestedSex() {
        for (Sex sex : Sex.values()) {
            HkidCard card = HkidCardUtil.generateRandomCard(REFERENCE_DATE, sex);

            assertGeneratedCard(card, REFERENCE_DATE);
            assertEquals(sex, card.getSex());
        }
    }

    @Test
    void requestedSexConvenienceOverloadsUseRequestedSex() {
        assertEquals(Sex.MALE, HkidCardUtil.generateRandomCard(Sex.MALE).getSex());
        assertEquals(
                Sex.FEMALE,
                HkidCardUtil.generateRandomCard(REFERENCE_DATE, Sex.FEMALE).getSex());
    }

    @ParameterizedTest
    @CsvSource({
            "17, BORN_IN_HONG_KONG, *AZ",
            "17, BORN_IN_MAINLAND, *AX",
            "18, BORN_IN_MACAO, ***AW",
            "18, BORN_ELSEWHERE, ***AO"
    })
    void generatesCompleteCardsAtRequestedAgeBoundsWithCompatiblePrefixes(
            int expectedAge, HkidSymbol birthplace, String expectedSymbols) {
        HkidCard card = HkidCardUtil.generateRandomCard(
                REFERENCE_DATE, Sex.FEMALE, expectedAge, expectedAge, birthplace);

        assertGeneratedCard(card, REFERENCE_DATE);
        assertEquals(Integer.valueOf(expectedAge), card.getAge(REFERENCE_DATE).orElse(null));
        assertEquals(expectedSymbols, card.getSymbolsString());
        assertEquals(Sex.FEMALE, card.getSex());
        assertTrue(card.findSuspicions().isEmpty());
        DefinedPrefix prefix = card.getHkidNumber().getDefinedPrefix().get();
        if (birthplace == HkidSymbol.BORN_IN_HONG_KONG) {
            assertTrue(supportsPossibleBirthRegistrationDate(prefix, card.getDateOfBirth()));
        } else {
            assertTrue(prefix.supportsFirstIssueMonth(
                    card.getFirstRegistrationYearMonth(), card.getDateOfRegistration()));
        }
    }

    @Test
    void generatesWithinRequestedAgeRange() {
        for (int i = 0; i < 30; i++) {
            HkidCard card = HkidCardUtil.generateRandomCard(
                    REFERENCE_DATE, 25, 35, HkidSymbol.BORN_ELSEWHERE);
            int age = card.getAge(REFERENCE_DATE).get();
            assertTrue(age >= 25 && age <= 35);
        }
    }

    @Test
    void constrainedConvenienceOverloadsUseRequestedValues() {
        HkidCard dated = HkidCardUtil.generateRandomCard(
                REFERENCE_DATE, 100, 100, HkidSymbol.BORN_ELSEWHERE);
        assertEquals(Integer.valueOf(100), dated.getAge(REFERENCE_DATE).orElse(null));
        assertEquals("***AO", dated.getSymbolsString());
        assertEquals("*AW", HkidCardUtil.generateRandomCard(
                11, 11, HkidSymbol.BORN_IN_MACAO).getSymbolsString());
    }

    @Test
    void rejectsUnsupportedAgeRangesAndBirthplaces() {
        HkidSymbol hongKong = HkidSymbol.BORN_IN_HONG_KONG;
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, 10, 12, hongKong));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, 99, 101, hongKong));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, 18, 17, hongKong));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, 11, 100, null));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, 11, 100, HkidSymbol.RIGHT_OF_ABODE));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(REFERENCE_DATE, null, 11, 100, hongKong));
    }

    @ParameterizedTest
    @CsvSource({
            "0, BORN_IN_HONG_KONG", "4574833, BORN_IN_HONG_KONG",
            "4574834, BORN_IN_MAINLAND", "6718959, BORN_IN_MAINLAND",
            "6718960, BORN_IN_MACAO", "6770934, BORN_IN_MACAO",
            "6770935, BORN_ELSEWHERE", "7413069, BORN_ELSEWHERE"
    })
    void census2021BirthplaceWeightsCoverEachInterval(int ticket, HkidSymbol expected) {
        assertEquals(expected, HkidCardUtil.censusBirthplaceForTicket(ticket));
    }

    @Test
    void censusTicketsSpanExactlyTheResidentPopulation() {
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.censusBirthplaceForTicket(-1));
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.censusBirthplaceForTicket(7_413_070));
    }

    @ParameterizedTest
    @ValueSource(ints = {17, 100})
    void generatesCompletePopulationBirthplaceCard(int age) {
        LocalDate reference = HkidCard.CURRENT_SMART_HKID_START_DATE;
        HkidCard card = HkidCardUtil.generateRandomCardByBirthplaceDistribution(reference, age, age);

        assertGeneratedCard(card, reference);
        assertEquals(Integer.valueOf(age), card.getAge(reference).orElse(null));
        assertEquals(reference, card.getDateOfRegistration());
        assertEquals(1, card.getSymbols().getByCategory(HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH).size());
        assertTrue(card.findSuspicions().isEmpty());
    }

    @Test
    void rejectsInvalidPopulationSamplingArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCardByBirthplaceDistribution(null, 11, 100));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCardByBirthplaceDistribution(REFERENCE_DATE, 18, 17));
    }

    @Test
    void toStringOnlyShowsMaskedHkidNumberAndDateOfRegistration() {
        HkidCard card = HkidCard.builder()
                .hkidNumber(new HkidNumber("A123456(3)"))
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .build();

        String value = card.toString();

        assertEquals("HkidCard[hkidNumber=****456(*), dateOfRegistration=01-06-20]", value);
        assertFalse(value.contains(card.getHkidNumberString(HkidNumber.Format.COMPLETE)));
    }

    @Test
    void generatedPrefixMatchesHongKongBirthRegistrationPeriod() {
        for (int age = 11; age <= 100; age++) {
            HkidCard card = HkidCardUtil.generateRandomCard(
                    REFERENCE_DATE, age, age, HkidSymbol.BORN_IN_HONG_KONG);
            LocalDate dateOfBirth = card.getDateOfBirth();
            DefinedPrefix prefix = card.getHkidNumber().getDefinedPrefix().orElse(null);

            assertNotNull(prefix);
            switch (prefix) {
                case Z:
                case Y:
                case S:
                case N:
                    assertTrue(supportsPossibleBirthRegistrationDate(prefix, dateOfBirth));
                    break;
                default:
                    assertTrue(prefix.supportsFirstIssueMonth(
                            card.getFirstRegistrationYearMonth(), card.getDateOfRegistration()));
                    break;
            }
        }
    }

    @Test
    void generatedFirstIssuePrefixesAreBoundedByEligibilityAndRegistrationDates() {
        YearMonth firstIssue = YearMonth.of(2026, 7);
        LocalDate july26 = firstIssue.atDay(26);
        LocalDate july27 = firstIssue.atDay(27);
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F}, HkidCardUtil.compatiblePrefixesFor(
                null, firstIssue, july26, july26));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.F, DefinedPrefix.L}, HkidCardUtil.compatiblePrefixesFor(
                null, firstIssue, july26, july27));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.L}, HkidCardUtil.compatiblePrefixesFor(
                null, firstIssue, july27, july27));
    }

    @ParameterizedTest
    @CsvSource({"2020-02-24, F", "2026-07-26, F", "2026-07-27, L", "2026-08-01, L"})
    void generatesCompatibleFirstIssuePrefixOnEleventhBirthday(LocalDate reference, DefinedPrefix expected) {
        HkidCard card = HkidCardUtil.generateCompleteCard(
                reference, null, reference.minusYears(11), HkidSymbol.BORN_IN_MAINLAND);

        assertEquals(reference, card.getDateOfRegistration());
        assertEquals(YearMonth.from(reference), card.getFirstRegistrationYearMonth());
        assertEquals(expected, card.getHkidNumber().getDefinedPrefix().get());
        card.validateAsOf(reference);
    }

    @Test
    void generatedBirthPrefixUsesBirthRegistrationDate() {
        YearMonth firstIssue = YearMonth.of(2004, 11);
        LocalDate registration = LocalDate.of(2026, 7, 21);
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.Z}, HkidCardUtil.compatiblePrefixesFor(
                LocalDate.of(1988, 12, 31), firstIssue, firstIssue.atDay(1), registration));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.Y}, HkidCardUtil.compatiblePrefixesFor(
                LocalDate.of(1989, 1, 1), firstIssue, firstIssue.atDay(1), registration));
        assertArrayEquals(new DefinedPrefix[]{DefinedPrefix.R}, HkidCardUtil.compatiblePrefixesFor(
                null, firstIssue, firstIssue.atDay(1), registration));
    }

    private static boolean supportsPossibleBirthRegistrationDate(
            DefinedPrefix prefix, LocalDate dateOfBirth) {
        for (int daysAfterBirth = 0; daysAfterBirth <= 42; daysAfterBirth++) {
            if (prefix.supportsHongKongBirthRegistrationDate(
                    dateOfBirth.plusDays(daysAfterBirth))) {
                return true;
            }
        }
        return false;
    }

    private void assertGeneratedCard(HkidCard hkidCard, LocalDate referenceDate) {
        assertNotNull(hkidCard.getHkidNumber());
        assertNotNull(hkidCard.getChineseName());
        assertNotNull(hkidCard.getEnglishName());
        assertNotNull(hkidCard.getSex());
        assertNotNull(hkidCard.getDateOfBirth());
        assertNotNull(hkidCard.getDateOfRegistration());
        assertNotNull(hkidCard.getFirstRegistrationYearMonth());
        LocalDate earliestCurrentRegistration = referenceDate.minusYears(10)
                .isAfter(HkidCard.CURRENT_SMART_HKID_START_DATE)
                ? referenceDate.minusYears(10)
                : HkidCard.CURRENT_SMART_HKID_START_DATE;
        assertFalse(hkidCard.getDateOfRegistration().isBefore(earliestCurrentRegistration));
        assertFalse(hkidCard.getDateOfRegistration().isAfter(referenceDate));
        assertFalse(hkidCard.getFirstRegistrationYearMonth()
                .isBefore(YearMonth.from(
                        HkidCard.ageAnniversary(hkidCard.getDateOfBirth(), 11))));
        assertFalse(hkidCard.getFirstRegistrationYearMonth()
                .isAfter(YearMonth.from(hkidCard.getDateOfRegistration())));
        String chineseName = hkidCard.getChineseNameString();
        // Each Han code point has one commercial code. A supplementary character uses two
        // UTF-16 units, so length() is only the end offset here, not the character count.
        assertEquals(chineseName.codePointCount(0, chineseName.length()),
                hkidCard.getChineseCommercialCodes().size());
        assertNameMatchesSex(hkidCard);
        hkidCard.validateAsOf(referenceDate);
    }

    private void assertNameMatchesSex(HkidCard hkidCard) {
        String personalName = hkidCard.getChineseName().getPersonalName();

        // charAt() would split supplementary Han characters; preserve each whole character
        // with codePoints() and Character.toChars() so it matches its seed entry.
        personalName.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));
            ChineseNameEntry entry = HkidNameUtil.getDefaultEntries().stream()
                    .filter(candidate -> character.equals(candidate.getCharacter()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
            assertTrue(entry.isCompatibleWith(hkidCard.getSex()));
        });
    }
}
