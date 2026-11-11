package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.number.HkidNumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HkidCardSuspicionTest {
    @Test
    void rejectsNullCardArguments() {
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.isSuspicious(null));
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.findSuspicions(null));
    }

    @Test
    void missingNumberOrOtherFieldsAreNotSuspicious() {
        assertSuspicions(HkidCard.builder().build());
        assertSuspicions(cardWithPrefix("Y").build());
        assertSuspicions(cardWithPrefix("M").build());
        assertSuspicions(HkidCard.builder()
                .dateOfBirth(LocalDate.of(2010, 1, 1))
                .symbolCodes("X")
                .firstRegistrationYearMonth(YearMonth.of(2024, 1))
                .dateOfRegistration(LocalDate.of(2025, 1, 1))
                .build());
    }

    @ParameterizedTest
    @CsvSource({
            "Z, 1988-12-31, false",
            "Z, 1989-01-01, true",
            "Z, 1989-01-02, true",
            "Y, 2005-03-31, false",
            "Y, 2005-04-01, true",
            "Y, 2005-04-02, true",
            "S, 2019-05-31, false",
            "S, 2019-06-01, true",
            "S, 2019-06-02, true"
    })
    void detectsBirthOnOrAfterTheBirthRegistrationSequenceEnd(
            String prefix, LocalDate birthDate, boolean suspicious) {
        HkidCard card = cardWithPrefix(prefix).dateOfBirth(birthDate).build();

        assertSuspicions(card, suspicious
                ? new HkidSuspicion[]{HkidSuspicion.PREFIX_BIRTH_DATE_MISMATCH}
                : new HkidSuspicion[0]);
    }

    @ParameterizedTest
    @CsvSource({
            "Z, 1979-12-31",
            "Y, 1988-12-31",
            "S, 2005-03-31",
            "N, 2019-05-31",
            "Z, 1960-01-01",
            "Y, 1960-01-01",
            "S, 1960-01-01",
            "N, 1960-01-01"
    })
    void allowsBirthBeforeSequenceIntroductionWithoutAssumingRegistrationWithin42Days(
            String prefix, LocalDate birthDate) {
        assertSuspicions(cardWithPrefix(prefix).dateOfBirth(birthDate).build());
    }

    @Test
    void openEndedBirthSequenceHasNoBirthDateUpperBound() {
        assertSuspicions(cardWithPrefix("N")
                .dateOfBirth(LocalDate.of(2099, 12, 31))
                .build());
    }

    @ParameterizedTest
    @CsvSource({
            "Z, X", "Z, W", "Z, O",
            "Y, X", "Y, W", "Y, O",
            "S, X", "S, W", "S, O",
            "N, X", "N, W", "N, O"
    })
    void detectsExplicitNonHongKongBirthplaceWithoutRequiringBirthDate(
            String prefix, String birthplaceCode) {
        assertSuspicions(cardWithPrefix(prefix).symbolCodes(birthplaceCode).build(),
                HkidSuspicion.PREFIX_BIRTH_PLACE_MISMATCH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z", "Y", "S", "N"})
    void acceptsHongKongBirthplaceForBirthRegistrationSequences(String prefix) {
        assertSuspicions(cardWithPrefix(prefix).symbolCodes("Z").build());
    }

    @Test
    void missingBirthplaceDoesNotPreventBirthDateCheck() {
        assertSuspicions(cardWithPrefix("Y")
                        .dateOfBirth(LocalDate.of(2010, 1, 1))
                        .build(),
                HkidSuspicion.PREFIX_BIRTH_DATE_MISMATCH);
    }

    @Test
    void ordinaryFirstIssuePrefixDoesNotImplyForeignBirthOrBirthDuringIssuancePeriod() {
        assertSuspicions(cardWithPrefix("M")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .symbolCodes("Z")
                .build());
    }

    @Test
    void birthDetailsChangedSuppressesBirthDateAndBirthplaceSuspicions() {
        assertSuspicions(cardWithPrefix("S")
                .dateOfBirth(LocalDate.of(2020, 1, 1))
                .symbolCodes("XB")
                .build());
    }

    @Test
    void nameChangedDoesNotSuppressBirthSuspicionsAndResultsHaveStableOrder() {
        HkidCard card = cardWithPrefix("Y")
                .dateOfBirth(LocalDate.of(2010, 1, 1))
                .symbolCodes("ON")
                .build();

        assertSuspicions(card,
                HkidSuspicion.PREFIX_BIRTH_DATE_MISMATCH,
                HkidSuspicion.PREFIX_BIRTH_PLACE_MISMATCH);
        assertEquals(HkidCardUtil.findSuspicions(card), HkidCardUtil.findSuspicions(card));
    }

    @ParameterizedTest
    @CsvSource({
            "K, 1983-03", "K, 1990-07",
            "P, 1990-08", "P, 2000-12",
            "R, 2000-12", "R, 2011-07",
            "M, 2011-08", "M, 2020-02",
            "F, 2020-02", "F, 2026-07"
    })
    void anyOverlappingDayMakesTheFirstRegistrationMonthCompatible(
            String prefix, YearMonth firstRegistrationMonth) {
        assertSuspicions(cardWithPrefix(prefix)
                .firstRegistrationYearMonth(firstRegistrationMonth)
                .build());
    }

    @ParameterizedTest
    @CsvSource({
            "K, 1983-02", "K, 1990-08",
            "P, 1990-07", "P, 2001-01",
            "R, 2000-11", "R, 2011-08",
            "M, 2011-07", "M, 2020-03",
            "F, 2020-01", "F, 2026-08"
    })
    void detectsFirstRegistrationOutsideSequenceWithoutRequiringBirthFields(
            String prefix, YearMonth firstRegistrationMonth) {
        assertSuspicions(cardWithPrefix(prefix)
                        .firstRegistrationYearMonth(firstRegistrationMonth)
                        .build(),
                HkidSuspicion.PREFIX_FIRST_REGISTRATION_MISMATCH);
    }

    @Test
    void birthDetailsChangedDoesNotSuppressFirstRegistrationCheck() {
        assertSuspicions(cardWithPrefix("M")
                        .symbolCodes("B")
                        .firstRegistrationYearMonth(YearMonth.of(2024, 1))
                        .build(),
                HkidSuspicion.PREFIX_FIRST_REGISTRATION_MISMATCH);
    }

    @Test
    void birthRegistrationPrefixIsNotComparedWithFirstHkidRegistrationMonth() {
        assertSuspicions(cardWithPrefix("Y")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .symbolCodes("Z")
                .firstRegistrationYearMonth(YearMonth.of(2024, 1))
                .build());
    }

    @Test
    void currentCardRegistrationDateDoesNotReplaceFirstRegistrationMonth() {
        assertSuspicions(cardWithPrefix("M")
                .firstRegistrationYearMonth(YearMonth.of(2015, 1))
                .dateOfRegistration(LocalDate.of(2026, 8, 1))
                .build());
        assertSuspicions(cardWithPrefix("F")
                .dateOfRegistration(LocalDate.of(2026, 8, 1))
                .build());
        assertSuspicions(cardWithPrefix("F")
                        .firstRegistrationYearMonth(YearMonth.of(2020, 1))
                        .dateOfRegistration(LocalDate.of(2026, 7, 1))
                        .build(),
                HkidSuspicion.PREFIX_FIRST_REGISTRATION_MISMATCH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA", "J", "T", "V", "W", "WX", "XA", "XB", "XC", "XD", "XE", "XG", "XH"})
    void unknownOrUndatedPrefixesAreSkipped(String prefix) {
        assertSuspicions(cardWithPrefix(prefix)
                .dateOfBirth(LocalDate.of(2020, 1, 1))
                .symbolCodes("O")
                .firstRegistrationYearMonth(YearMonth.of(2024, 1))
                .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1990-01", "2025-01", "2026-07", "2027-01"})
    void skipsLBecauseItsOlderReserveIssuancePeriodIsNotFullyModelled(YearMonth firstRegistrationMonth) {
        assertSuspicions(cardWithPrefix("L")
                .firstRegistrationYearMonth(firstRegistrationMonth)
                .build());
    }

    @Test
    void resultsAreImmutableAndCheckingPreservesTheCard() {
        HkidCard card = cardWithPrefix("Y")
                .dateOfBirth(LocalDate.of(2010, 1, 1))
                .symbolCodes("X")
                .build();
        HkidCard original = card.toBuilder().build();
        List<HkidSuspicion> issues = HkidCardUtil.findSuspicions(card);
        List<HkidSuspicion> emptyIssues = HkidCardUtil.findSuspicions(HkidCard.builder().build());

        assertThrows(UnsupportedOperationException.class,
                () -> issues.add(HkidSuspicion.PREFIX_FIRST_REGISTRATION_MISMATCH));
        assertThrows(UnsupportedOperationException.class, () -> issues.remove(0));
        assertThrows(UnsupportedOperationException.class,
                () -> emptyIssues.add(HkidSuspicion.PREFIX_BIRTH_DATE_MISMATCH));
        assertEquals(original, card);
    }

    @Test
    void generatedCardsDoNotProduceSuspicions() {
        LocalDate referenceDate = LocalDate.of(2026, 11, 11);
        for (int seed = 0; seed < 20; seed++) {
            assertSuspicions(HkidCardUtil.generateRandomCard(new Random(seed), referenceDate));
        }
    }

    private static HkidCard.Builder cardWithPrefix(String prefix) {
        return HkidCard.builder().hkidNumber(new HkidNumber(prefix, "123456"));
    }

    private static void assertSuspicions(HkidCard card, HkidSuspicion... expected) {
        assertEquals(Arrays.asList(expected), HkidCardUtil.findSuspicions(card));
        assertEquals(expected.length > 0, HkidCardUtil.isSuspicious(card));
        assertEquals(Arrays.asList(expected), card.findSuspicions());
        assertEquals(expected.length > 0, card.isSuspicious());
    }
}
