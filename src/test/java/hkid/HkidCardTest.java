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
