package hkid;

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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String prefix;

        if (onlyDefinedPrefix) {
            HkidNum.DefinedPrefix[] prefixes = HkidNum.DefinedPrefix.values();
            prefix = prefixes[random.nextInt(prefixes.length)].name();
        } else {
            int prefixLength = random.nextInt(1, 3);
            StringBuilder builder = new StringBuilder(prefixLength);
            for (int i = 0; i < prefixLength; i++) {
                builder.append((char) random.nextInt('A', 'Z' + 1));
            }
            prefix = builder.toString();
        }

        String numerals = String.format("%06d", random.nextInt(1_000_000));
        return new HkidNum(prefix, numerals);
    }
}
