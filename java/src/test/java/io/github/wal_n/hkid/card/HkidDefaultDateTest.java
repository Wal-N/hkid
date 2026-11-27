package io.github.wal_n.hkid.card;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("java.util.TimeZone.default")
class HkidDefaultDateTest {
    private static final ZoneOffset[] SYSTEM_ZONES = {
            ZoneOffset.ofHours(-18), ZoneOffset.ofHours(14)
    };

    @Test
    void cardGeneratorsResolveSystemTimeZoneOnEachCall() {
        TimeZone original = TimeZone.getDefault();
        AtomicInteger zoneLookups = new AtomicInteger();
        LocalDate referenceDate = LocalDate.of(2026, 7, 21);
        try {
            for (ZoneOffset zone : SYSTEM_ZONES) {
                // Observe every call without relying on random outputs landing on date boundaries.
                TimeZone.setDefault(new SimpleTimeZone(zone.getTotalSeconds() * 1000, zone.getId()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public ZoneId toZoneId() {
                        zoneLookups.incrementAndGet();
                        return zone;
                    }
                });
                for (boolean requestSex : new boolean[]{false, true}) {
                    LocalDate before = LocalDate.now(zone);
                    int lookupsBefore = zoneLookups.get();
                    HkidCard generated = requestSex
                            ? HkidCardUtil.generateRandomCard(Sex.FEMALE)
                            : HkidCardUtil.generateRandomCard();
                    LocalDate after = LocalDate.now(zone);

                    assertTrue(zoneLookups.get() > lookupsBefore,
                            "Each default-date call must resolve the current system time zone");
                    generated.validateAsOf(after);
                    assertFalse(generated.getDateOfRegistration().isBefore(before.minusYears(10)));
                    // Allow either local date if the call crossed midnight.
                    assertTrue(generated.getAge(after).get() >= 11);
                    assertTrue(generated.getAge(before).get() <= 85);
                    if (requestSex) {
                        assertEquals(Sex.FEMALE, generated.getSex());
                    }
                }

                int lookupsBefore = zoneLookups.get();
                HkidCardUtil.generateRandomCard(referenceDate).validateAsOf(referenceDate);
                assertEquals(lookupsBefore, zoneLookups.get(),
                        "An explicit reference date must not consult the system time zone");
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void defaultDateRangeEndsAtSystemTodayExclusively() {
        TimeZone original = TimeZone.getDefault();
        try {
            for (ZoneOffset zone : SYSTEM_ZONES) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                LocalDate before = LocalDate.now(zone);
                LocalDate yesterday = before.minusDays(1);
                LocalDate generated = HkidCardUtil.generateRandomDateInRange(yesterday);
                LocalDate after = LocalDate.now(zone);

                assertFalse(generated.isBefore(yesterday));
                assertTrue(generated.isBefore(after));
                if (before.equals(after)) {
                    assertEquals(yesterday, generated);
                }

                LocalDate today = LocalDate.now(zone);
                LocalDate result = null;
                IllegalArgumentException rejection = null;
                try {
                    result = HkidCardUtil.generateRandomDateInRange(today);
                } catch (IllegalArgumentException expected) {
                    rejection = expected;
                }
                LocalDate dateAfterCall = LocalDate.now(zone);
                if (today.equals(dateAfterCall)) {
                    assertNotNull(rejection, "An empty range ending today must be rejected");
                } else if (rejection == null) {
                    // A call that crosses system midnight may have a nonempty range.
                    assertFalse(result.isBefore(today));
                    assertTrue(result.isBefore(dateAfterCall));
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
