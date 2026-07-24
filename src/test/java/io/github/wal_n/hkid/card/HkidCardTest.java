package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.number.DefinedPrefix;
import io.github.wal_n.hkid.number.HkidNumber;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidCardTest {
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 21);

    @Test
    void generatesCompleteCardAtLowerRandomBoundaries() {
        HkidCard hkidCard = HkidCardUtil.generateRandomCard(new BoundaryRandom(false), REFERENCE_DATE);

        assertGeneratedCard(hkidCard);
        assertEquals(Sex.MALE, hkidCard.getSex());
        assertEquals(11, Period.between(hkidCard.getDateOfBirth(), REFERENCE_DATE).getYears());
        assertEquals(hkidCard.getDateOfBirth().plusYears(11), hkidCard.getDateOfRegistration());
        assertEquals(YearMonth.from(hkidCard.getDateOfRegistration()), hkidCard.getFirstRegistrationYearMonth());
        assertEquals(HkidSymbols.parse("*AZ"), hkidCard.getSymbols());

        System.out.println(hkidCard);
        System.out.println("HKID Number: " + hkidCard.getHkidNumber());
        System.out.println("Age: " + hkidCard.getAge(REFERENCE_DATE));
    }

    @Test
    void generatesCompleteCardAtUpperRandomBoundaries() {
        HkidCard hkidCard = HkidCardUtil.generateRandomCard(new BoundaryRandom(true), REFERENCE_DATE);

        assertGeneratedCard(hkidCard);
        assertEquals(Sex.FEMALE, hkidCard.getSex());
        assertEquals(100, Period.between(hkidCard.getDateOfBirth(), REFERENCE_DATE).getYears());
        assertEquals(REFERENCE_DATE, hkidCard.getDateOfRegistration());
        assertEquals(YearMonth.from(REFERENCE_DATE), hkidCard.getFirstRegistrationYearMonth());
        assertEquals(HkidSymbols.parse("***AZ"), hkidCard.getSymbols());
    }

    @Test
    void rejectsMissingRandomGenerationDependencies() {
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.generateRandomCard((LocalDate) null));
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.generateRandomCard(null, REFERENCE_DATE));
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.generateRandomCard(new Random(), null));
    }

    @Test
    void rejectsReferenceDateBeforeCurrentSmartHkidStartDate() {
        LocalDate unsupportedDate = HkidCard.CURRENT_SMART_HKID_START_DATE.minusDays(1);

        IllegalArgumentException threadLocalException = assertThrows(
                IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(unsupportedDate));
        IllegalArgumentException deterministicException = assertThrows(
                IllegalArgumentException.class,
                () -> HkidCardUtil.generateRandomCard(new Random(1L), unsupportedDate));

        assertEquals(
                "Reference date cannot be before " + HkidCard.CURRENT_SMART_HKID_START_DATE,
                threadLocalException.getMessage());
        assertEquals(threadLocalException.getMessage(), deterministicException.getMessage());
    }

    @Test
    void acceptsCurrentSmartHkidStartDate() {
        HkidCard card = HkidCardUtil.generateRandomCard(
                new BoundaryRandom(false),
                HkidCard.CURRENT_SMART_HKID_START_DATE);

        assertEquals(HkidCard.CURRENT_SMART_HKID_START_DATE, card.getDateOfRegistration());
        card.validateAsOf(HkidCard.CURRENT_SMART_HKID_START_DATE);
    }

    @Test
    void generatesCompleteCardForCallerControlledReferenceDate() {
        HkidCard hkidCard = HkidCardUtil.generateRandomCard(REFERENCE_DATE);

        assertGeneratedCard(hkidCard);
    }

    @Test
    void sameSeedGeneratesSameCompleteCard() {
        HkidCard first = HkidCardUtil.generateRandomCard(new Random(123456789L), REFERENCE_DATE);
        HkidCard second = HkidCardUtil.generateRandomCard(new Random(123456789L), REFERENCE_DATE);

        assertEquals(first, second);
    }

    @Test
    void toStringOnlyShowsMaskedHkidNumberAndDateOfRegistration() {
        HkidCard card = HkidCard.builder()
                .hkidNumber(new HkidNumber("A123456(3)"))
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .build();

        String value = card.toString();

        assertEquals("HkidCard[hkidNumber=****456(*), dateOfRegistration=01-06-20]", value);
        assertFalse(value.contains(card.getHkidNumberStr(HkidNumber.Format.Complete)));
    }

    @Test
    void generatedPrefixMatchesHongKongBirthRegistrationPeriod() {
        for (int seed = 0; seed < 500; seed++) {
            HkidCard card = HkidCardUtil.generateRandomCard(new Random(seed), REFERENCE_DATE);
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
                    assertTrue(prefix.supportsFirstIssueMonth(card.getFirstRegistrationYearMonth()));
                    break;
            }
        }
    }

    @Test
    void generatedPrefixMatchesFirstRegistrationMonth() {
        HkidCard card = HkidCardUtil.generateRandomCard(
                new Random(41), LocalDate.of(2026, 7, 21));

        assertEquals(LocalDate.of(1978, 4, 25), card.getDateOfBirth());
        assertEquals(YearMonth.of(2004, 11), card.getFirstRegistrationYearMonth());
        assertEquals(DefinedPrefix.R, card.getHkidNumber().getDefinedPrefix().orElse(null));
        assertTrue(DefinedPrefix.R.supportsFirstIssueMonth(
                card.getFirstRegistrationYearMonth()));
    }

    @Test
    void generatedBirthPrefixUsesRegistrationDateAfterDateOfBirth() {
        LocalDate referenceDate = LocalDate.of(2026, 7, 21);
        LocalDate dateOfBirth = LocalDate.of(1988, 12, 31);
        LocalDate earliestDateOfBirthForAge = referenceDate.minusYears(38).plusDays(1);
        int dateOfBirthOffset = Math.toIntExact(
                ChronoUnit.DAYS.between(earliestDateOfBirthForAge, dateOfBirth));
        Random random = new SequenceRandom(26, dateOfBirthOffset, 0, 0, 1);

        HkidCard card = HkidCardUtil.generateRandomCard(random, referenceDate);

        assertEquals(dateOfBirth, card.getDateOfBirth());
        assertEquals(DefinedPrefix.Y, card.getHkidNumber().getDefinedPrefix().orElse(null));
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

    private void assertGeneratedCard(HkidCard hkidCard) {
        assertNotNull(hkidCard.getHkidNumber());
        assertNotNull(hkidCard.getChineseName());
        assertNotNull(hkidCard.getEnglishName());
        assertNotNull(hkidCard.getDateOfBirth());
        assertNotNull(hkidCard.getDateOfRegistration());
        assertNotNull(hkidCard.getFirstRegistrationYearMonth());
        LocalDate earliestCurrentRegistration = REFERENCE_DATE.minusYears(10)
                .isAfter(HkidCard.CURRENT_SMART_HKID_START_DATE)
                ? REFERENCE_DATE.minusYears(10)
                : HkidCard.CURRENT_SMART_HKID_START_DATE;
        assertFalse(hkidCard.getDateOfRegistration().isBefore(earliestCurrentRegistration));
        assertFalse(hkidCard.getDateOfRegistration().isAfter(REFERENCE_DATE));
        assertFalse(hkidCard.getFirstRegistrationYearMonth()
                .isBefore(YearMonth.from(hkidCard.getDateOfBirth().plusYears(11))));
        assertFalse(hkidCard.getFirstRegistrationYearMonth()
                .isAfter(YearMonth.from(hkidCard.getDateOfRegistration())));
        assertEquals(hkidCard.getChineseName().length(), hkidCard.getChineseCommercialCodes().size());
        hkidCard.validateAsOf(REFERENCE_DATE);
    }

    private static final class BoundaryRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final boolean upperBoundary;

        private BoundaryRandom(boolean upperBoundary) {
            this.upperBoundary = upperBoundary;
        }

        @Override
        public int nextInt(int bound) {
            return upperBoundary ? bound - 1 : 0;
        }
    }

    private static final class SequenceRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int[] values;
        private int index;

        private SequenceRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = index < values.length ? values[index++] : 0;
            if (value < 0 || value >= bound) {
                throw new AssertionError(String.format("Random value %s is outside bound %s", value, bound));
            }
            return value;
        }
    }
}
