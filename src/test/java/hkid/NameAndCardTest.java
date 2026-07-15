package hkid;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameAndCardTest {
    @Test
    void chiNameValidatesUpdatedSurnameAndPersonalNameTogether() {
        ChiName name = new ChiName();
        name.setSurname("陳");
        name.setPersonalName("大文");
        name.setCommercialCodes(Arrays.asList("1234", "5678", "9999"));

        assertEquals("陳大文", name.getFullName());
        assertThrows(IllegalArgumentException.class, () -> name.setPersonalName("大文一二三四"));
        assertThrows(IllegalArgumentException.class, () -> name.setSurname("Chan"));
        assertThrows(IllegalArgumentException.class, () -> name.setCommercialCodes(Arrays.asList("1234", "5678")));
    }

    @Test
    void engNameValidatesNameParts() {
        EngName name = new EngName("Chan", "Tai Man");

        assertEquals("Chan, Tai Man", name.getFullName());
        assertThrows(IllegalArgumentException.class, () -> name.setSurname("123"));
    }

    @Test
    void hkidReturnsEmptyCommercialCodeListWhenChineseNameIsMissing() {
        HKID hkid = new HKID();

        assertTrue(hkid.getChiCommercialCode().isEmpty());
    }

    @Test
    void hkidProtectsSymbolListFromExternalMutation() {
        HKID hkid = new HKID();
        hkid.setSymbols(Arrays.asList(DefinedSymbol.ThreeStars, DefinedSymbol.A));

        assertThrows(UnsupportedOperationException.class, () -> hkid.getSymbols().add(DefinedSymbol.Z));
        assertEquals(Arrays.asList(DefinedSymbol.ThreeStars, DefinedSymbol.A), hkid.getSymbols());
    }

    @Test
    void hkidValidatesDateOrder() {
        HKID hkid = new HKID();
        hkid.setDateOfBirth(LocalDate.of(1990, 1, 1));
        hkid.setDateOfRegistration(LocalDate.of(2005, 6, 1));
        hkid.setDateOfIssue(YearMonth.of(2005, 6));

        assertThrows(IllegalArgumentException.class, () -> hkid.setDateOfBirth(LocalDate.now().plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> hkid.setDateOfRegistration(LocalDate.of(1989, 12, 31)));
        assertThrows(IllegalArgumentException.class, () -> hkid.setDateOfIssue(YearMonth.of(2005, 5)));
    }

    @Test
    void sexParsesCaseInsensitiveCode() {
        assertEquals(Sex.MALE, Sex.fromCode("m"));
        assertEquals("F", Sex.FEMALE.toString());
        assertThrows(IllegalArgumentException.class, () -> Sex.fromCode("X"));
    }

    @Test
    void definedSymbolExposesPrintedValue() {
        assertEquals("***", DefinedSymbol.ThreeStars.getStr());
        assertEquals("***", DefinedSymbol.ThreeStars.toString());
        assertFalse(DefinedSymbol.A.getDescription().isEmpty());
    }

    @Test
    void generatedHkidContainsExpectedSampleData() {
        HKID hkid = HKIDUtil.genRandomHkid();

        assertEquals("陳大文", hkid.getChiName());
        assertEquals("Chan, Tai Man", hkid.getEngName());
        assertEquals(Sex.MALE, hkid.getSex());
        assertEquals(Arrays.asList("1234", "5678", "9999"), hkid.getChiCommercialCode());
        assertEquals(Arrays.asList(DefinedSymbol.ThreeStars, DefinedSymbol.A, DefinedSymbol.Z), hkid.getSymbols());
    }
}
