package io.github.wal_n.hkid.name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Utility methods for generating Chinese names, commercial codes, and romanised English names.
 */
public final class HkidNameUtil {
    private static final String DEFAULT_SEED_RESOURCE =
            "io/github/wal_n/hkid/name/chinese-name-seed.csv";
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
     * Generates a name using caller-controlled random state and the default
     * personal-name length distribution.
     *
     * @param random random generator used for every generated name value
     * @return a generated Chinese name with matching commercial codes and English name
     * @throws IllegalArgumentException if {@code random} is null
     */
    public static GeneratedName generateRandomName(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }

        int roll = random.nextInt(100);
        return generateRandomName(defaultPersonalNameLengthForRoll(roll), random);
    }

    private static GeneratedName generateRandomName(int requestedPersonalNameLength, Random random) {
        validateGeneratedPersonalNameLength(requestedPersonalNameLength);

        List<ChineseNameEntry> surnameEntries = filter(DEFAULT_ENTRIES, ChineseNameEntry::isCommonSurname);
        List<ChineseNameEntry> givenNameEntries = filter(DEFAULT_ENTRIES, entry -> !entry.isCommonSurname());

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
        InputStream inputStream = HkidNameUtil.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalStateException("Name seed resource not found: " + resourceName);
        }

        List<ChineseNameEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.equals("commercialCode,character,romanisation,commonSurname,weight")) {
                    continue;
                }

                List<String> fields = parseCsvLine(line);
                if (fields.size() != 5) {
                    throw new IllegalStateException("Invalid name seed row at line " + lineNumber + ": " + line);
                }

                entries.add(new ChineseNameEntry(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        parseBoolean(fields.get(3), lineNumber),
                        Integer.parseInt(fields.get(4))));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load name seed resource: " + resourceName, e);
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

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        fields.add(field.toString().trim());
        return fields;
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

    private static void validateGeneratedPersonalNameLength(int requestedPersonalNameLength) {
        if (requestedPersonalNameLength < 1
                || requestedPersonalNameLength > MAX_GENERATED_PERSONAL_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Generated personal name length must be between 1 and "
                            + MAX_GENERATED_PERSONAL_NAME_LENGTH);
        }
    }
}
