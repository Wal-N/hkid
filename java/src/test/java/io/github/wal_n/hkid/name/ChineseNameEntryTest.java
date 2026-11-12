package io.github.wal_n.hkid.name;

import io.github.wal_n.hkid.card.Sex;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("java.util.Locale.default")
class ChineseNameEntryTest {
    @ParameterizedTest
    @CsvSource({"WING, Wing", "Wing, Wing", "ip, Ip", "' wInG ', Wing"})
    void normalisesRomanisationUnderTurkishDefaultLocale(String input, String expected) {
        Locale original = Locale.getDefault();
        Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            ChineseNameEntry entry = new ChineseNameEntry(
                    "0001", "永", input, false, EnumSet.allOf(Sex.class), 1);

            assertEquals(expected, entry.getRomanisation());
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT, originalFormat);
        }
    }
}
