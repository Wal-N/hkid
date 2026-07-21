package hkid;

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
    private static final String DEFAULT_SEED_RESOURCE = "hkid/chinese-name-seed.csv";
    private static final int ONE_CHARACTER_PERSONAL_NAME_PERCENTAGE = 10;
    private static final List<ChineseNameEntry> DEFAULT_ENTRIES = loadEntries(DEFAULT_SEED_RESOURCE);

    private HkidNameUtil() {
        throw new AssertionError("HkidNameUtil cannot be instantiated");
    }

    public static GeneratedName genRandomName() {
        return genRandomName(ThreadLocalRandom.current());
    }

    public static GeneratedName genRandomName(int personalNameLength) {
        return genRandomName(personalNameLength, ThreadLocalRandom.current());
    }

    static GeneratedName genRandomName(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }

        int roll = random.nextInt(100);
        return genRandomName(personalNameLengthForRoll(roll), random);
    }

    private static GeneratedName genRandomName(int personalNameLength, Random random) {
        validatePersonalNameLength(personalNameLength);

        List<ChineseNameEntry> surnameEntries = filter(DEFAULT_ENTRIES, ChineseNameEntry::isCommonSurname);
        List<ChineseNameEntry> givenNameEntries = filter(DEFAULT_ENTRIES, entry -> !entry.isCommonSurname());

        if (surnameEntries.isEmpty()) {
            throw new IllegalStateException("No surname seed entries are available");
        }
        if (givenNameEntries.size() < personalNameLength) {
            throw new IllegalStateException("Not enough given name seed entries are available");
        }

        ChineseNameEntry surname = weightedRandom(surnameEntries, random);
        List<ChineseNameEntry> personalNameEntries = new ArrayList<>();
        List<ChineseNameEntry> remainingGivenNameEntries = new ArrayList<>(givenNameEntries);
        for (int i = 0; i < personalNameLength; i++) {
            ChineseNameEntry entry = weightedRandom(remainingGivenNameEntries, random);
            personalNameEntries.add(entry);
            remainingGivenNameEntries.remove(entry);
        }

        return buildGeneratedName(surname, personalNameEntries);
    }

    public static List<ChineseNameEntry> getDefaultEntries() {
        return DEFAULT_ENTRIES;
    }

    static int personalNameLengthForRoll(int roll) {
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
        List<String> romanisation = new ArrayList<>();
        List<String> englishGivenName = new ArrayList<>();

        commercialCodes.add(surname.getCommercialCode());
        romanisation.add(surname.getRomanisation());

        for (ChineseNameEntry entry : personalNameEntries) {
            personalName.append(entry.getCharacter());
            commercialCodes.add(entry.getCommercialCode());
            romanisation.add(entry.getRomanisation());
            englishGivenName.add(entry.getRomanisation());
        }

        ChiName chiName = new ChiName(surname.getCharacter(), personalName.toString(), commercialCodes);
        EngName engName = new EngName(surname.getRomanisation(), String.join(" ", englishGivenName));
        return new GeneratedName(chiName, engName, romanisation);
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

    private static void validatePersonalNameLength(int personalNameLength) {
        if (personalNameLength < 1 || personalNameLength > 2) {
            throw new IllegalArgumentException("Personal name length must be 1 or 2");
        }
    }
}
