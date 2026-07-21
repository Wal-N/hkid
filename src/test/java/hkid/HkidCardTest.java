package hkid;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HkidCardTest {
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    void generatesCompleteCardAtLowerRandomBoundaries() {
        HkidCard hkidCard = HkidCardUtil.genRandomCard(new BoundaryRandom(false), TODAY);

        assertGeneratedCard(hkidCard);
        assertEquals(Sex.MALE, hkidCard.getSex());
        assertEquals(11, Period.between(hkidCard.getDateOfBirth(), TODAY).getYears());
        assertEquals(hkidCard.getDateOfBirth().plusYears(11), hkidCard.getDateOfRegistration());
        assertEquals(YearMonth.from(hkidCard.getDateOfRegistration()), hkidCard.getFirstRegistrationYearMonth());
        assertEquals(HkidSymbols.parse("*AZ"), hkidCard.getSymbols());

        System.out.println(hkidCard);
        System.out.println("HKID Number: " + hkidCard.getHkidNum());
        System.out.println("Age: " + hkidCard.getAge());
    }

    @Test
    void generatesCompleteCardAtUpperRandomBoundaries() {
        HkidCard hkidCard = HkidCardUtil.genRandomCard(new BoundaryRandom(true), TODAY);

        assertGeneratedCard(hkidCard);
        assertEquals(Sex.FEMALE, hkidCard.getSex());
        assertEquals(100, Period.between(hkidCard.getDateOfBirth(), TODAY).getYears());
        assertEquals(TODAY, hkidCard.getDateOfRegistration());
        assertEquals(YearMonth.from(TODAY), hkidCard.getFirstRegistrationYearMonth());
        assertEquals(HkidSymbols.parse("***AZ"), hkidCard.getSymbols());
    }

    @Test
    void rejectsMissingRandomGenerationDependencies() {
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.genRandomCard(null, TODAY));
        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.genRandomCard(new Random(), null));
    }

    @Test
    void sameSeedGeneratesSameCompleteCard() {
        HkidCard first = HkidCardUtil.genRandomCard(new Random(123456789L), TODAY);
        HkidCard second = HkidCardUtil.genRandomCard(new Random(123456789L), TODAY);

        assertEquals(first.toString(), second.toString());
    }

    @Test
    void toStringOnlyShowsMaskedHkidNumberAndDateOfRegistration() {
        HkidCard card = new HkidCard();
        card.setHkidNum(new HkidNum("A123456(3)"));
        card.setDateOfRegistration(LocalDate.of(2020, 6, 1));

        String value = card.toString();

        assertEquals("HkidCard[hkidNum=****456(*), dateOfRegistration=01-06-20]", value);
        assertFalse(value.contains(card.getHkidNumStr(HkidNum.Format.Complete)));
    }

    @Test
    void generatedPrefixMatchesHongKongBirthRegistrationPeriod() {
        for (int seed = 0; seed < 500; seed++) {
            HkidCard card = HkidCardUtil.genRandomCard(new Random(seed), TODAY);
            LocalDate dateOfBirth = card.getDateOfBirth();
            HkidNum.DefinedPrefix prefix = card.getHkidNum().getDefinedPrefix().orElse(null);

            assertNotNull(prefix);
            switch (prefix) {
                case Z:
                case Y:
                case S:
                case N:
                    assertTrue(prefix.supportsHongKongBirthDate(dateOfBirth));
                    break;
                default:
                    break;
            }
        }
    }

    private void assertGeneratedCard(HkidCard hkidCard) {
        assertNotNull(hkidCard.getHkidNum());
        assertNotNull(hkidCard.getChiName());
        assertNotNull(hkidCard.getEngName());
        assertNotNull(hkidCard.getDateOfBirth());
        assertNotNull(hkidCard.getDateOfRegistration());
        assertNotNull(hkidCard.getFirstRegistrationYearMonth());
        LocalDate earliestCurrentRegistration = TODAY.minusYears(10).isAfter(HkidCard.CURRENT_SMART_HKID_START_DATE)
                ? TODAY.minusYears(10)
                : HkidCard.CURRENT_SMART_HKID_START_DATE;
        assertFalse(hkidCard.getDateOfRegistration().isBefore(earliestCurrentRegistration));
        assertFalse(hkidCard.getDateOfRegistration().isAfter(TODAY));
        assertFalse(hkidCard.getFirstRegistrationYearMonth()
                .isBefore(YearMonth.from(hkidCard.getDateOfBirth().plusYears(11))));
        assertFalse(hkidCard.getFirstRegistrationYearMonth()
                .isAfter(YearMonth.from(hkidCard.getDateOfRegistration())));
        assertEquals(hkidCard.getChiName().length(), hkidCard.getChiCommercialCode().size());
        hkidCard.validateAsOf(TODAY);
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
}
