package io.github.wal_n.hkid.name;

import io.github.wal_n.hkid.card.Sex;
import io.github.wal_n.hkid.internal.ResourceCsv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Utility methods for generating Chinese names, commercial codes, and romanised English names.
 */
public final class HkidNameUtil {
    private static final String DEFAULT_SEED_RESOURCE =
            "io/github/wal_n/hkid/data/chinese-name-seed.csv";
    private static final int ONE_CHARACTER_PERSONAL_NAME_PERCENTAGE = 10;
    private static final int MAX_GENERATED_PERSONAL_NAME_LENGTH = ChineseName.MAX_LENGTH - 1;
    private static final List<ChineseNameEntry> DEFAULT_ENTRIES = loadEntries(DEFAULT_SEED_RESOURCE);

    private HkidNameUtil() {
        throw new AssertionError("HkidNameUtil cannot be instantiated");
    }

    /**
     * Generates a name using the default personal-name length distribution and
     * a thread-local random generator.
     *
     * @return a generated Chinese name with matching commercial codes and English name
     */
    public static GeneratedName generateRandomName() {
        return generateRandomName(ThreadLocalRandom.current());
    }

    /**
     * Generates a name with the requested number of Chinese characters in the
     * personal-name portion.
     *
     * <p>The generator uses a one-character surname, so the supported personal
     * name length is one to five characters under {@link ChineseName#MAX_LENGTH}.
     * This generator limit does not define the structure of all Hong Kong
     * Chinese names.</p>
     *
     * @param requestedPersonalNameLength number of generated personal-name characters
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if the requested length is outside the supported range
     */
    public static GeneratedName generateRandomName(int requestedPersonalNameLength) {
        return generateRandomName(requestedPersonalNameLength, ThreadLocalRandom.current());
    }

    /**
     * Generates a name associated with the requested sex using the default
     * personal-name length distribution.
     *
     * <p>Unisex seed entries remain eligible for male and female names.</p>
     *
     * @param sex requested generated-name sex
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if {@code sex} is null
     */
    public static GeneratedName generateRandomName(Sex sex) {
        return generateRandomName(sex, ThreadLocalRandom.current());
    }

    /**
     * Generates a name associated with the requested sex and with the
     * requested number of Chinese personal-name characters.
     *
     * @param requestedPersonalNameLength number of generated personal-name characters
     * @param sex requested generated-name sex
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if the length is unsupported or {@code sex} is null
     */
    public static GeneratedName generateRandomName(
            int requestedPersonalNameLength, Sex sex) {
        return generateRandomName(
                requestedPersonalNameLength, sex, ThreadLocalRandom.current());
    }

    /**
     * Generates a name using caller-controlled random state and the default
     * personal-name length distribution.
     *
     * @param random random generator used for every generated name value
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if {@code random} is null
     */
    public static GeneratedName generateRandomName(Random random) {
        validateRandom(random);

        int roll = random.nextInt(100);
        return generateRandomName(defaultPersonalNameLengthForRoll(roll), random);
    }

    /**
     * Generates a sex-associated name using caller-controlled random state
     * and the default personal-name length distribution.
     *
     * @param sex requested generated-name sex
     * @param random random generator used for every generated name value
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if either argument is null
     */
    public static GeneratedName generateRandomName(Sex sex, Random random) {
        validateSex(sex);
        validateRandom(random);

        int roll = random.nextInt(100);
        return generateRandomName(defaultPersonalNameLengthForRoll(roll), sex, random);
    }

    /**
     * Generates a sex-associated name with a requested personal-name length
     * using caller-controlled random state.
     *
     * @param requestedPersonalNameLength number of generated personal-name characters
     * @param sex requested generated-name sex
     * @param random random generator used for every generated name value
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if the length is unsupported or either object is null
     */
    public static GeneratedName generateRandomName(
            int requestedPersonalNameLength, Sex sex, Random random) {
        validateSex(sex);
        validateRandom(random);
        return generateRandomName(
                requestedPersonalNameLength,
                random,
                entry -> entry.isCompatibleWith(sex));
    }

    private static GeneratedName generateRandomName(int requestedPersonalNameLength, Random random) {
        return generateRandomName(requestedPersonalNameLength, random, entry -> true);
    }

    private static GeneratedName generateRandomName(
            int requestedPersonalNameLength,
            Random random,
            Predicate<ChineseNameEntry> givenNamePredicate) {
        validateGeneratedPersonalNameLength(requestedPersonalNameLength);

        List<ChineseNameEntry> surnameEntries = filter(DEFAULT_ENTRIES, ChineseNameEntry::isCommonSurname);
        List<ChineseNameEntry> givenNameEntries = filter(
                DEFAULT_ENTRIES,
                entry -> !entry.isCommonSurname()
                        && givenNamePredicate.test(entry));

        if (surnameEntries.isEmpty()) {
            throw new IllegalStateException("No surname seed entries are available");
        }
        if (givenNameEntries.size() < requestedPersonalNameLength) {
            throw new IllegalStateException("Not enough given name seed entries are available");
        }

        ChineseNameEntry surname = weightedRandom(surnameEntries, random);
        List<ChineseNameEntry> personalNameEntries = new ArrayList<>();
        List<ChineseNameEntry> remainingGivenNameEntries = new ArrayList<>(givenNameEntries);
        for (int i = 0; i < requestedPersonalNameLength; i++) {
            ChineseNameEntry entry = weightedRandom(remainingGivenNameEntries, random);
            personalNameEntries.add(entry);
            remainingGivenNameEntries.remove(entry);
        }

        return buildGeneratedName(surname, personalNameEntries);
    }

    /**
     * Returns the bundled entries used by the default random-name generator.
     *
     * @return an unmodifiable list of Chinese character seed entries
     */
    public static List<ChineseNameEntry> getDefaultEntries() {
        return DEFAULT_ENTRIES;
    }

    static int defaultPersonalNameLengthForRoll(int roll) {
        if (roll < 0 || roll >= 100) {
            throw new IllegalArgumentException("Random roll must be between 0 and 99");
        }
        return roll < ONE_CHARACTER_PERSONAL_NAME_PERCENTAGE ? 1 : 2;
    }

    static List<ChineseNameEntry> loadEntries(String resourceName) {
        List<ChineseNameEntry> entries = new ArrayList<>();
        List<ResourceCsv.Row> rows = ResourceCsv.readRows(
                resourceName,
                "commercialCode",
                "character",
                "romanisation",
                "commonSurname",
                "sexAssociation",
                "weight");
        try {
            for (ResourceCsv.Row row : rows) {
                List<String> fields = row.getFields();
                entries.add(new ChineseNameEntry(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        parseBoolean(fields.get(3), row.getLineNumber()),
                        parseSupportedSexes(fields.get(4), row.getLineNumber()),
                        Integer.parseInt(fields.get(5))));
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to parse name seed resource: " + resourceName, e);
        }

        return Collections.unmodifiableList(entries);
    }

    private static GeneratedName buildGeneratedName(ChineseNameEntry surname, List<ChineseNameEntry> personalNameEntries) {
        StringBuilder personalName = new StringBuilder();
        List<String> commercialCodes = new ArrayList<>();
        List<String> englishGivenName = new ArrayList<>();

        commercialCodes.add(surname.getCommercialCode());

        for (ChineseNameEntry entry : personalNameEntries) {
            personalName.append(entry.getCharacter());
            commercialCodes.add(entry.getCommercialCode());
            englishGivenName.add(entry.getRomanisation());
        }

        ChineseName chineseName = new ChineseName(
                surname.getCharacter(), personalName.toString(), commercialCodes);
        EnglishName englishName = new EnglishName(
                surname.getRomanisation(), String.join(" ", englishGivenName));
        return new GeneratedName(chineseName, englishName);
    }

    private static ChineseNameEntry weightedRandom(List<ChineseNameEntry> entries, Random random) {
        int totalWeight = 0;
        for (ChineseNameEntry entry : entries) {
            totalWeight += entry.getWeight();
        }

        int selectedWeight = random.nextInt(totalWeight);
        for (ChineseNameEntry entry : entries) {
            selectedWeight -= entry.getWeight();
            if (selectedWeight < 0) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private static List<ChineseNameEntry> filter(List<ChineseNameEntry> entries, Predicate<ChineseNameEntry> predicate) {
        List<ChineseNameEntry> filtered = new ArrayList<>();
        for (ChineseNameEntry entry : entries) {
            if (predicate.test(entry)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private static boolean parseBoolean(String value, int lineNumber) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean at line " + lineNumber + ": " + value);
    }

    private static Set<Sex> parseSupportedSexes(String value, int lineNumber) {
        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        if ("UNISEX".equalsIgnoreCase(normalizedValue)) {
            return EnumSet.allOf(Sex.class);
        }
        try {
            return EnumSet.of(Sex.valueOf(normalizedValue));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid sex association at line " + lineNumber + ": " + value, e);
        }
    }

    private static void validateSex(Sex sex) {
        if (sex == null) {
            throw new IllegalArgumentException("Sex cannot be null");
        }
    }

    private static void validateRandom(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }
    }

    private static void validateGeneratedPersonalNameLength(int requestedPersonalNameLength) {
        if (requestedPersonalNameLength < 1
                || requestedPersonalNameLength > MAX_GENERATED_PERSONAL_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Generated personal name length must be between 1 and "
                            + MAX_GENERATED_PERSONAL_NAME_LENGTH);
        }
    }
}
