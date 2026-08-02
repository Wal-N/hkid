package io.github.wal_n.hkid.card;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, validated collection of symbols printed on a current smart HKID card.
 */
public final class HkidSymbols {
    private static final HkidSymbols EMPTY = new HkidSymbols(Collections.emptyList());

    private final List<HkidSymbol> symbols;

    private HkidSymbols(Collection<HkidSymbol> symbols) {
        if (symbols == null) {
            throw new IllegalArgumentException("HKID symbols cannot be null");
        }

        EnumSet<HkidSymbol> uniqueSymbols = EnumSet.noneOf(HkidSymbol.class);
        Map<HkidSymbolCategory, HkidSymbol> exclusiveSymbols =
                new EnumMap<>(HkidSymbolCategory.class);

        for (HkidSymbol symbol : symbols) {
            if (symbol == null) {
                throw new IllegalArgumentException("HKID symbol cannot be null");
            }
            if (!uniqueSymbols.add(symbol)) {
                throw new IllegalArgumentException("Duplicate HKID symbol: " + symbol.getCode());
            }
            if (!symbol.getCategory().allowsMultiple()) {
                HkidSymbol existing = exclusiveSymbols.put(symbol.getCategory(), symbol);
                if (existing != null) {
                    throw new IllegalArgumentException(
                            "Conflicting HKID symbols: " + existing.getCode() + " and " + symbol.getCode());
                }
            }
        }

        this.symbols = Collections.unmodifiableList(new ArrayList<>(uniqueSymbols));
    }

    /**
     * Returns the shared empty symbol collection.
     *
     * @return an empty immutable collection
     */
    public static HkidSymbols empty() {
        return EMPTY;
    }

    /**
     * Creates a validated immutable collection from symbol values.
     *
     * @param symbols symbols to include
     * @return the validated symbol collection
     * @throws IllegalArgumentException if the array or an entry is null, a symbol
     *         is duplicated, or mutually exclusive categories conflict
     */
    public static HkidSymbols of(HkidSymbol... symbols) {
        if (symbols == null) {
            throw new IllegalArgumentException("HKID symbols cannot be null");
        }
        return of(Arrays.asList(symbols));
    }

    /**
     * Creates a validated immutable collection from a symbol collection.
     *
     * @param symbols symbols to include
     * @return the validated symbol collection
     * @throws IllegalArgumentException if the collection or an entry is null, a
     *         symbol is duplicated, or mutually exclusive categories conflict
     */
    public static HkidSymbols of(Collection<HkidSymbol> symbols) {
        if (symbols == null) {
            throw new IllegalArgumentException("HKID symbols cannot be null");
        }
        return symbols.isEmpty() ? EMPTY : new HkidSymbols(symbols);
    }

    /**
     * Parses concatenated current smart-HKID symbol codes.
     *
     * @param value printed symbol string; surrounding whitespace is ignored
     * @return the parsed immutable symbol collection
     * @throws IllegalArgumentException if the value is null, contains an unsupported
     *         code, duplicates a symbol, or combines mutually exclusive symbols
     */
    public static HkidSymbols parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("HKID symbol string cannot be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            return EMPTY;
        }

        List<HkidSymbol> parsedSymbols = new ArrayList<>();
        int index = 0;
        while (index < normalizedValue.length()) {
            if (normalizedValue.startsWith(HkidSymbol.ADULT_RE_ENTRY_PERMIT.getCode(), index)) {
                parsedSymbols.add(HkidSymbol.ADULT_RE_ENTRY_PERMIT);
                index += HkidSymbol.ADULT_RE_ENTRY_PERMIT.getCode().length();
            } else if (normalizedValue.charAt(index) == '*') {
                parsedSymbols.add(HkidSymbol.MINOR_RE_ENTRY_PERMIT);
                index++;
            } else {
                parsedSymbols.add(HkidSymbol.fromCode(String.valueOf(normalizedValue.charAt(index))));
                index++;
            }
        }
        return of(parsedSymbols);
    }

    /**
     * Returns the symbols in their canonical enum order.
     *
     * @return an unmodifiable symbol list
     */
    public List<HkidSymbol> asList() {
        return symbols;
    }

    /**
     * Returns all symbols in the requested category.
     *
     * @param category category to select
     * @return an unmodifiable list of matching symbols
     * @throws IllegalArgumentException if {@code category} is null
     */
    public List<HkidSymbol> getByCategory(HkidSymbolCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("HKID symbol category cannot be null");
        }

        List<HkidSymbol> matchingSymbols = new ArrayList<>();
        for (HkidSymbol symbol : symbols) {
            if (symbol.getCategory() == category) {
                matchingSymbols.add(symbol);
            }
        }
        return Collections.unmodifiableList(matchingSymbols);
    }

    /**
     * Tests whether this collection contains a symbol.
     *
     * @param symbol symbol to find
     * @return {@code true} when the symbol is present
     */
    public boolean contains(HkidSymbol symbol) {
        return symbols.contains(symbol);
    }

    /**
     * Tests whether this collection contains no symbols.
     *
     * @return {@code true} when the collection is empty
     */
    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    /**
     * Validates an age-specific Re-entry Permit symbol on a reference date.
     *
     * @param dateOfBirth holder's date of birth
     * @param referenceDate date on which to calculate the holder's age
     * @throws IllegalArgumentException if either date is null, the birth date is
     *         after the reference date, or an age symbol does not match the age
     */
    public void validateAge(LocalDate dateOfBirth, LocalDate referenceDate) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        if (dateOfBirth.isAfter(referenceDate)) {
            throw new IllegalArgumentException("Date of birth cannot be after the reference date");
        }

        Optional<HkidSymbol> eligibilitySymbol = symbols.stream()
                .filter(symbol -> symbol.getCategory() == HkidSymbolCategory.RE_ENTRY_PERMIT_ELIGIBILITY)
                .findFirst();
        if (!eligibilitySymbol.isPresent()) {
            return;
        }

        int age = HkidCard.ageOn(dateOfBirth, referenceDate);
        if (eligibilitySymbol.get() == HkidSymbol.ADULT_RE_ENTRY_PERMIT && age < 18) {
            throw new IllegalArgumentException("The *** symbol requires the holder to be aged 18 or over");
        }
        if (eligibilitySymbol.get() == HkidSymbol.MINOR_RE_ENTRY_PERMIT
                && (age < 11 || age > 17)) {
            throw new IllegalArgumentException("The * symbol requires the holder to be aged between 11 and 17");
        }
    }

    @Override
    public String toString() {
        StringBuilder value = new StringBuilder();
        for (HkidSymbol symbol : symbols) {
            value.append(symbol.getCode());
        }
        return value.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HkidSymbols)) {
            return false;
        }
        HkidSymbols other = (HkidSymbols) object;
        return symbols.equals(other.symbols);
    }

    @Override
    public int hashCode() {
        return symbols.hashCode();
    }
}
