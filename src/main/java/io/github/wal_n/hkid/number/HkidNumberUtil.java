package io.github.wal_n.hkid.number;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for working with HKID numbers.
 */
public final class HkidNumberUtil {
    private HkidNumberUtil() {
        throw new AssertionError("HkidNumberUtil cannot be instantiated");
    }

    /**
     * Generates an HKID number using a predefined prefix.
     */
    public static HkidNumber generateRandomHkidNumber() {
        return generateRandomHkidNumber(true);
    }

    /**
     * Generates an HKID number, optionally restricted to predefined prefixes.
     */
    public static HkidNumber generateRandomHkidNumber(boolean onlyDefinedPrefix) {
        return generateRandomHkidNumber(onlyDefinedPrefix, ThreadLocalRandom.current());
    }

    /**
     * Masks an HKID number, leaving only the last three numerals visible.
     * The prefix, first three numerals, and check digit are replaced by asterisks.
     *
     * @param hkidNumber HKID number to mask
     * @return the masked HKID number, or {@code null} when {@code hkidNumber} is null
     */
    public static String maskHkidNumber(HkidNumber hkidNumber) {
        if (hkidNumber == null) {
            return null;
        }
        String prefixAndLeadingNumeralsMask = hkidNumber.getPrefix().length() == 2 ? "*****" : "****";
        return prefixAndLeadingNumeralsMask + hkidNumber.getNumerals().substring(3) + "(*)";
    }

    public static HkidNumber generateRandomHkidNumber(
            Random random, DefinedPrefix... allowedPrefixes) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }
        if (allowedPrefixes == null || allowedPrefixes.length == 0) {
            throw new IllegalArgumentException("At least one allowed prefix is required");
        }

        DefinedPrefix prefix = allowedPrefixes[random.nextInt(allowedPrefixes.length)];
        if (prefix == null) {
            throw new IllegalArgumentException("Allowed prefixes cannot contain null");
        }
        return buildRandomHkidNumber(prefix.name(), random);
    }

    private static HkidNumber generateRandomHkidNumber(boolean onlyDefinedPrefix, Random random) {
        String prefix;

        if (onlyDefinedPrefix) {
            DefinedPrefix[] prefixes = DefinedPrefix.values();
            prefix = prefixes[random.nextInt(prefixes.length)].name();
        } else {
            int prefixLength = random.nextInt(2) + 1;
            StringBuilder builder = new StringBuilder(prefixLength);
            for (int i = 0; i < prefixLength; i++) {
                builder.append((char) ('A' + random.nextInt('Z' - 'A' + 1)));
            }
            prefix = builder.toString();
        }

        return buildRandomHkidNumber(prefix, random);
    }

    private static HkidNumber buildRandomHkidNumber(String prefix, Random random) {
        String numerals = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        return new HkidNumber(prefix, numerals);
    }
}
