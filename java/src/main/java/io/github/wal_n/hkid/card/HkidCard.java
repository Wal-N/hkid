package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.EnglishName;
import io.github.wal_n.hkid.number.HkidNumber;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable data printed on a Hong Kong Identity Card.
 *
 * <p>Use {@link #builder()} to assemble a card. Time-dependent operations require
 * an explicit reference date so that a card's behaviour does not change merely
 * because the system date changed.</p>
 */
public final class HkidCard {
    static final LocalDate CURRENT_SMART_HKID_START_DATE = LocalDate.of(2018, 11, 26);

    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FIRST_REGISTRATION_YEAR_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MM-yy");
    private static final DateTimeFormatter DOR_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");

    private final HkidNumber hkidNumber;
    private final ChineseName chineseName;
    private final EnglishName englishName;
    private final Sex sex;
    private final LocalDate dateOfBirth;
    private final HkidSymbols symbols;
    private final YearMonth firstRegistrationYearMonth;
    private final LocalDate dateOfRegistration;

    private HkidCard(Builder builder) {
        this.hkidNumber = builder.hkidNumber;
        this.chineseName = new ChineseName(
                builder.chineseSurname,
                builder.chinesePersonalName,
                builder.chineseCommercialCodes);
        this.englishName = new EnglishName(
                builder.englishSurname,
                builder.englishPersonalName);
        this.sex = builder.sex;
        this.dateOfBirth = builder.dateOfBirth;
        this.symbols = builder.symbols;
        this.firstRegistrationYearMonth = builder.firstRegistrationYearMonth;
        this.dateOfRegistration = builder.dateOfRegistration;
        validateCardFields();
    }

    /**
     * Creates a builder with empty optional card fields and no symbols.
     *
     * @return a new card builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder pre-populated with this card's values.
     *
     * @return a builder for producing a modified immutable card
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return String.format("%s[hkidNumber=%s, dateOfRegistration=%s]",
                getClass().getSimpleName(),
                getHkidNumberMaskedStr(),
                getDateOfRegistrationStr());
    }

    /**
     * Returns the holder's age on the supplied date.
     *
     * @param referenceDate date on which the age is required
     * @return an empty optional when no date of birth is present
     */
    public Optional<Integer> getAge(LocalDate referenceDate) {
        requireReferenceDate(referenceDate);
        if (dateOfBirth != null && dateOfBirth.isAfter(referenceDate)) {
            throw new IllegalArgumentException("Date of birth cannot be after the reference date");
        }
        return Optional.ofNullable(dateOfBirth)
                .map(dob -> ageOn(dob, referenceDate));
    }

    // Match LocalDate.plusYears(): February 29 anniversaries fall on
    // February 28 in non-leap years.
    static int ageOn(LocalDate dateOfBirth, LocalDate referenceDate) {
        int age = referenceDate.getYear() - dateOfBirth.getYear();
        return referenceDate.isBefore(dateOfBirth.plusYears(age)) ? age - 1 : age;
    }

    /**
     * Returns the card's HKID number.
     *
     * @return the HKID number, or {@code null} when it was not supplied
     */
    public HkidNumber getHkidNumber() {
        return hkidNumber;
    }

    /**
     * Returns the HKID prefix and numerals without the check digit.
     *
     * @return the unadorned HKID number, or {@code null} when absent
     */
    public String getHkidNumberStr() {
        return getHkidNumberStr(HkidNumber.Format.WITHOUT_CHECK_DIGIT);
    }

    /**
     * Returns the HKID number in the requested format.
     *
     * @param format output format; {@code null} selects
     *         {@link HkidNumber.Format#WITHOUT_CHECK_DIGIT}
     * @return the formatted HKID number, or {@code null} when absent
     */
    public String getHkidNumberStr(HkidNumber.Format format) {
        return hkidNumber != null ? hkidNumber.toString(format) : null;
    }

    /**
     * Returns a privacy-masked HKID number with only the last three numerals visible.
     *
     * @return the masked HKID number, or {@code null} when absent
     */
    public String getHkidNumberMaskedStr() {
        return hkidNumber != null ? hkidNumber.toMaskedString() : null;
    }

    /**
     * Returns the printed Chinese name. Use {@link #getChineseNameInfo()} when surname,
     * personal name, or commercial codes are needed separately.
     *
     * @return the complete Chinese name, possibly empty
     */
    public String getChineseName() {
        return chineseName.getFullName();
    }

    /**
     * Returns the structured Chinese name value.
     *
     * @return the immutable Chinese name
     */
    public ChineseName getChineseNameInfo() {
        return chineseName;
    }

    /**
     * Returns the Chinese surname.
     *
     * @return the surname, possibly empty
     */
    public String getChineseSurname() {
        return chineseName.getSurname();
    }

    /**
     * Returns the Chinese personal name.
     *
     * @return the personal name, possibly empty
     */
    public String getChinesePersonalName() {
        return chineseName.getPersonalName();
    }

    /**
     * Returns the Chinese commercial codes in printed-name order.
     *
     * @return an unmodifiable list of commercial codes
     */
    public List<String> getChineseCommercialCodes() {
        return chineseName.getCommercialCodes();
    }

    /**
     * Returns the printed English name. Use {@link #getEnglishNameInfo()} when surname
     * and personal name are needed separately.
     *
     * @return the formatted English name, possibly empty
     */
    public String getEnglishName() {
        return englishName.getFullName();
    }

    /**
     * Returns the structured English name value.
     *
     * @return the immutable English name
     */
    public EnglishName getEnglishNameInfo() {
        return englishName;
    }

    /**
     * Returns the English surname.
     *
     * @return the surname, possibly empty
     */
    public String getEnglishSurname() {
        return englishName.getSurname();
    }

    /**
     * Returns the English personal name.
     *
     * @return the personal name, possibly empty
     */
    public String getEnglishPersonalName() {
        return englishName.getPersonalName();
    }

    /**
     * Returns the card's sex marker value.
     *
     * @return the sex value, or {@code null} when absent
     */
    public Sex getSex() {
        return sex;
    }

    /**
     * Returns the Traditional Chinese sex marker.
     *
     * @return the Chinese marker, or {@code null} when sex is absent
     */
    public String getSexChiMarker() {
        return sex != null ? sex.getChiMarker() : null;
    }

    /**
     * Returns the English sex marker.
     *
     * @return the English marker, or {@code null} when sex is absent
     */
    public String getSexEngMarker() {
        return sex != null ? sex.getEngMarker() : null;
    }

    /**
     * Returns the sex value as printed on the smart HKID card, for example
     * {@code "男 M"}.
     *
     * @return the combined printed value, or {@code null} when sex is absent
     */
    public String getSexPrintedValue() {
        return sex != null ? sex.getPrintedValue() : null;
    }

    /**
     * Returns the holder's date of birth.
     *
     * @return the date of birth, or {@code null} when absent
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Formats the date of birth as {@code dd-MM-yyyy}.
     *
     * @return the formatted date, or {@code null} when absent
     */
    public String getDateOfBirthStr() {
        return dateOfBirth != null ? dateOfBirth.format(DOB_FORMATTER) : null;
    }

    /**
     * Returns the validated symbols printed on the current smart HKID card.
     *
     * @return the immutable symbol collection
     */
    public HkidSymbols getSymbols() {
        return symbols;
    }

    /**
     * Returns the concatenated symbol codes as printed on the card.
     *
     * @return the symbol-code string, possibly empty
     */
    public String getSymbolCodes() {
        return symbols.toString();
    }

    /**
     * Returns the month of the holder's first HKID registration.
     *
     * @return the first-registration month, or {@code null} when absent
     */
    public YearMonth getFirstRegistrationYearMonth() {
        return firstRegistrationYearMonth;
    }

    /**
     * Formats the first-registration month as {@code MM-yy}.
     *
     * @return the formatted month, or {@code null} when absent
     */
    public String getFirstRegistrationYearMonthStr() {
        return firstRegistrationYearMonth != null
                ? firstRegistrationYearMonth.format(FIRST_REGISTRATION_YEAR_MONTH_FORMATTER)
                : null;
    }

    /**
     * Returns the current smart card's registration date.
     *
     * @return the registration date, or {@code null} when absent
     */
    public LocalDate getDateOfRegistration() {
        return dateOfRegistration;
    }

    /**
     * Formats the current smart card's registration date as {@code dd-MM-yy}.
     *
     * @return the formatted date, or {@code null} when absent
     */
    public String getDateOfRegistrationStr() {
        return dateOfRegistration != null ? dateOfRegistration.format(DOR_FORMATTER) : null;
    }

    /**
     * Validates that the card's dated fields are not in the future relative to
     * the supplied date. Card-face consistency, including age-dependent symbols,
     * is checked against the date of registration during construction.
     *
     * @param referenceDate date on which the card is being checked
     */
    public void validateAsOf(LocalDate referenceDate) {
        requireReferenceDate(referenceDate);
        if (dateOfBirth != null && dateOfBirth.isAfter(referenceDate)) {
            throw new IllegalArgumentException("Date of birth cannot be after the reference date");
        }
        if (dateOfRegistration != null && dateOfRegistration.isAfter(referenceDate)) {
            throw new IllegalArgumentException("Date of registration cannot be after the reference date");
        }
        if (firstRegistrationYearMonth != null
                && firstRegistrationYearMonth.isAfter(YearMonth.from(referenceDate))) {
            throw new IllegalArgumentException("First registration month cannot be after the reference date");
        }
    }

    private void validateCardFields() {
        if (dateOfRegistration != null && dateOfRegistration.isBefore(CURRENT_SMART_HKID_START_DATE)) {
            throw new IllegalArgumentException(
                    "Current smart HKID registration date cannot be before "
                            + CURRENT_SMART_HKID_START_DATE);
        }
        if (dateOfBirth != null && dateOfRegistration != null
                && dateOfRegistration.isBefore(dateOfBirth)) {
            throw new IllegalArgumentException("Date of registration cannot be before date of birth");
        }
        if (dateOfBirth != null && firstRegistrationYearMonth != null
                && firstRegistrationYearMonth.isBefore(YearMonth.from(dateOfBirth))) {
            throw new IllegalArgumentException("First registration month cannot be before date of birth");
        }
        if (dateOfRegistration != null && firstRegistrationYearMonth != null
                && firstRegistrationYearMonth.isAfter(YearMonth.from(dateOfRegistration))) {
            throw new IllegalArgumentException("First registration month cannot be after date of registration");
        }
        if (dateOfBirth != null && dateOfRegistration != null) {
            symbols.validateAge(dateOfBirth, dateOfRegistration);
        }
    }

    private static void requireReferenceDate(LocalDate referenceDate) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HkidCard)) {
            return false;
        }
        HkidCard other = (HkidCard) object;
        return Objects.equals(hkidNumber, other.hkidNumber)
                && chineseName.equals(other.chineseName)
                && englishName.equals(other.englishName)
                && sex == other.sex
                && Objects.equals(dateOfBirth, other.dateOfBirth)
                && symbols.equals(other.symbols)
                && Objects.equals(firstRegistrationYearMonth, other.firstRegistrationYearMonth)
                && Objects.equals(dateOfRegistration, other.dateOfRegistration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hkidNumber,
                chineseName,
                englishName,
                sex,
                dateOfBirth,
                symbols,
                firstRegistrationYearMonth,
                dateOfRegistration);
    }

    /**
     * Mutable construction aid for the immutable {@link HkidCard} model.
     */
    public static final class Builder {
        private HkidNumber hkidNumber;
        private String chineseSurname = "";
        private String chinesePersonalName = "";
        private List<String> chineseCommercialCodes = Collections.emptyList();
        private String englishSurname = "";
        private String englishPersonalName = "";
        private Sex sex;
        private LocalDate dateOfBirth;
        private HkidSymbols symbols = HkidSymbols.empty();
        private YearMonth firstRegistrationYearMonth;
        private LocalDate dateOfRegistration;

        private Builder() {
        }

        private Builder(HkidCard card) {
            this.hkidNumber = card.hkidNumber;
            this.chineseSurname = card.chineseName.getSurname();
            this.chinesePersonalName = card.chineseName.getPersonalName();
            this.chineseCommercialCodes = card.chineseName.getCommercialCodes();
            this.englishSurname = card.englishName.getSurname();
            this.englishPersonalName = card.englishName.getPersonalName();
            this.sex = card.sex;
            this.dateOfBirth = card.dateOfBirth;
            this.symbols = card.symbols;
            this.firstRegistrationYearMonth = card.firstRegistrationYearMonth;
            this.dateOfRegistration = card.dateOfRegistration;
        }

        /**
         * Sets the HKID number.
         *
         * @param hkidNumber number to store, or {@code null} to clear it
         * @return this builder
         */
        public Builder hkidNumber(HkidNumber hkidNumber) {
            this.hkidNumber = hkidNumber;
            return this;
        }

        /**
         * Sets all structured Chinese-name fields.
         *
         * @param chineseName name to copy, or {@code null} to clear all Chinese-name fields
         * @return this builder
         */
        public Builder chineseName(ChineseName chineseName) {
            ChineseName value = chineseName != null ? chineseName : new ChineseName();
            this.chineseSurname = value.getSurname();
            this.chinesePersonalName = value.getPersonalName();
            this.chineseCommercialCodes = value.getCommercialCodes();
            return this;
        }

        /**
         * Sets the Chinese surname.
         *
         * @param chineseSurname surname to store; {@code null} becomes empty when built
         * @return this builder
         */
        public Builder chineseSurname(String chineseSurname) {
            this.chineseSurname = chineseSurname;
            return this;
        }

        /**
         * Sets the Chinese personal name.
         *
         * @param chinesePersonalName personal name to store; {@code null} becomes empty when built
         * @return this builder
         */
        public Builder chinesePersonalName(String chinesePersonalName) {
            this.chinesePersonalName = chinesePersonalName;
            return this;
        }

        /**
         * Sets the Chinese commercial codes in printed-name order.
         *
         * @param chineseCommercialCodes codes to copy when built, or {@code null} for none
         * @return this builder
         */
        public Builder chineseCommercialCodes(List<String> chineseCommercialCodes) {
            this.chineseCommercialCodes = chineseCommercialCodes;
            return this;
        }

        /**
         * Sets all structured English-name fields.
         *
         * @param englishName name to copy, or {@code null} to clear all English-name fields
         * @return this builder
         */
        public Builder englishName(EnglishName englishName) {
            EnglishName value = englishName != null ? englishName : new EnglishName();
            this.englishSurname = value.getSurname();
            this.englishPersonalName = value.getPersonalName();
            return this;
        }

        /**
         * Sets the English surname.
         *
         * @param englishSurname surname to store; {@code null} becomes empty when built
         * @return this builder
         */
        public Builder englishSurname(String englishSurname) {
            this.englishSurname = englishSurname;
            return this;
        }

        /**
         * Sets the English personal name.
         *
         * @param englishPersonalName personal name to store; {@code null} becomes empty when built
         * @return this builder
         */
        public Builder englishPersonalName(String englishPersonalName) {
            this.englishPersonalName = englishPersonalName;
            return this;
        }

        /**
         * Sets the sex marker.
         *
         * @param sex sex value to store, or {@code null} to clear it
         * @return this builder
         */
        public Builder sex(Sex sex) {
            this.sex = sex;
            return this;
        }

        /**
         * Sets the sex marker from its English card value.
         *
         * @param sexEngMarker {@code M} or {@code F}, or {@code null} to clear the value
         * @return this builder
         * @throws IllegalArgumentException if a non-null marker is unsupported
         */
        public Builder sexEngMarker(String sexEngMarker) {
            this.sex = sexEngMarker != null ? Sex.fromEngMarker(sexEngMarker) : null;
            return this;
        }

        /**
         * Sets the holder's date of birth.
         *
         * @param dateOfBirth date to store, or {@code null} to clear it
         * @return this builder
         */
        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        /**
         * Sets the validated current smart-HKID symbols.
         *
         * @param symbols immutable symbol collection
         * @return this builder
         * @throws IllegalArgumentException if {@code symbols} is null
         */
        public Builder symbols(HkidSymbols symbols) {
            if (symbols == null) {
                throw new IllegalArgumentException("HKID symbols cannot be null");
            }
            this.symbols = symbols;
            return this;
        }

        /**
         * Parses and sets concatenated current smart-HKID symbol codes.
         *
         * @param symbolCodes symbol-code string to parse
         * @return this builder
         * @throws IllegalArgumentException if the string is null or invalid
         */
        public Builder symbolCodes(String symbolCodes) {
            return symbols(HkidSymbols.parse(symbolCodes));
        }

        /**
         * Sets the holder's first HKID registration month.
         *
         * @param firstRegistrationYearMonth month to store, or {@code null} to clear it
         * @return this builder
         */
        public Builder firstRegistrationYearMonth(YearMonth firstRegistrationYearMonth) {
            this.firstRegistrationYearMonth = firstRegistrationYearMonth;
            return this;
        }

        /**
         * Sets the current smart card's registration date.
         *
         * @param dateOfRegistration date to store, or {@code null} to clear it
         * @return this builder
         */
        public Builder dateOfRegistration(LocalDate dateOfRegistration) {
            this.dateOfRegistration = dateOfRegistration;
            return this;
        }

        /**
         * Creates and validates an immutable card from the current builder values.
         *
         * @return the constructed card
         * @throws IllegalArgumentException if a name, date relationship, or age-specific
         *         symbol is invalid
         */
        public HkidCard build() {
            return new HkidCard(this);
        }
    }
}
