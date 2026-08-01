package io.github.wal_n.hkid.conformance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConformanceFixturesTest {
    @Test
    void rejectsFractionalSchemaVersion() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ConformanceFixtures.cases(
                        "fractional-schema-version.json",
                        "cases"));

        assertEquals(
                "Unsupported or missing schemaVersion in fractional-schema-version.json",
                exception.getMessage());
    }
}
