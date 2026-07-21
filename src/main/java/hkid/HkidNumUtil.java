package hkid;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for generating HKID numbers.
 */
public final class HkidNumUtil {
    private HkidNumUtil() {
        throw new AssertionError("HkidNumUtil cannot be instantiated");
    }

    /**
     * Generates an HKID number using a predefined prefix.
     */
    public static HkidNum genRandomHkidNum() {
        return genRandomHkidNum(true);
    }

    /**
     * Generates an HKID number, optionally restricted to predefined prefixes.
     */
    public static HkidNum genRandomHkidNum(boolean onlyDefinedPrefix) {
        return genRandomHkidNum(onlyDefinedPrefix, ThreadLocalRandom.current());
    }

    static HkidNum genRandomHkidNum(Random random, HkidNum.DefinedPrefix... allowedPrefixes) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }
        if (allowedPrefixes == null || allowedPrefixes.length == 0) {
            throw new IllegalArgumentException("At least one allowed prefix is required");
        }

        HkidNum.DefinedPrefix prefix = allowedPrefixes[random.nextInt(allowedPrefixes.length)];
        if (prefix == null) {
            throw new IllegalArgumentException("Allowed prefixes cannot contain null");
        }
        return buildRandomHkidNum(prefix.name(), random);
    }

    private static HkidNum genRandomHkidNum(boolean onlyDefinedPrefix, Random random) {
        String prefix;

        if (onlyDefinedPrefix) {
            HkidNum.DefinedPrefix[] prefixes = HkidNum.DefinedPrefix.values();
            prefix = prefixes[random.nextInt(prefixes.length)].name();
        } else {
            int prefixLength = random.nextInt(2) + 1;
            StringBuilder builder = new StringBuilder(prefixLength);
            for (int i = 0; i < prefixLength; i++) {
                builder.append((char) ('A' + random.nextInt('Z' - 'A' + 1)));
            }
            prefix = builder.toString();
        }

        return buildRandomHkidNum(prefix, random);
    }

    private static HkidNum buildRandomHkidNum(String prefix, Random random) {
        String numerals = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        return new HkidNum(prefix, numerals);
    }
}
