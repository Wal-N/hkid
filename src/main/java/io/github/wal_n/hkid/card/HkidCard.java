package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseName;
import io.github.wal_n.hkid.name.EnglishName;
import io.github.wal_n.hkid.number.HkidNumber;

import java.time.LocalDate;
import java.time.Period;
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

    public static Builder builder() {
        return new Builder();
    }

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
                .map(dob -> Period.between(dob, referenceDate).getYears());
    }

    public HkidNumber getHkidNumber() {
        return hkidNumber;
    }

    public String getHkidNumberStr() {
        return getHkidNumberStr(HkidNumber.Format.WITHOUT_CHECK_DIGIT);
    }

    public String getHkidNumberStr(HkidNumber.Format format) {
        return hkidNumber != null ? hkidNumber.toString(format) : null;
    }

    public String getHkidNumberMaskedStr() {
        return hkidNumber != null ? hkidNumber.toMaskedString() : null;
    }

    /**
     * Returns the printed Chinese name. Use {@link #getChineseNameInfo()} when surname,
     * personal name, or commercial codes are needed separately.
     */
    public String getChineseName() {
        return chineseName.getFullName();
    }

    public ChineseName getChineseNameInfo() {
        return chineseName;
    }

    public String getChineseSurname() {
        return chineseName.getSurname();
    }

    public String getChinesePersonalName() {
        return chineseName.getPersonalName();
    }

    public List<String> getChineseCommercialCodes() {
        return chineseName.getCommercialCodes();
    }

    /**
     * Returns the printed English name. Use {@link #getEnglishNameInfo()} when surname
     * and personal name are needed separately.
     */
    public String getEnglishName() {
        return englishName.getFullName();
    }

    public EnglishName getEnglishNameInfo() {
        return englishName;
    }

    public String getEnglishSurname() {
        return englishName.getSurname();
    }

    public String getEnglishPersonalName() {
        return englishName.getPersonalName();
    }

    public Sex getSex() {
        return sex;
    }

    public String getSexChiMarker() {
        return sex != null ? sex.getChiMarker() : null;
    }

    public String getSexEngMarker() {
        return sex != null ? sex.getEngMarker() : null;
    }

    /**
     * Returns the sex value as printed on the smart HKID card, for example "男 M".
     */
    public String getSexPrintedValue() {
        return sex != null ? sex.getPrintedValue() : null;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDateOfBirthStr() {
        return dateOfBirth != null ? dateOfBirth.format(DOB_FORMATTER) : null;
    }

    /**
     * Returns the validated symbols printed on the current smart HKID card.
     */
    public HkidSymbols getSymbols() {
        return symbols;
    }

    public String getSymbolCodes() {
        return symbols.toString();
    }

    public YearMonth getFirstRegistrationYearMonth() {
        return firstRegistrationYearMonth;
    }

    public String getFirstRegistrationYearMonthStr() {
        return firstRegistrationYearMonth != null
                ? firstRegistrationYearMonth.format(FIRST_REGISTRATION_YEAR_MONTH_FORMATTER)
                : null;
    }

    public LocalDate getDateOfRegistration() {
        return dateOfRegistration;
    }

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

        public Builder hkidNumber(HkidNumber hkidNumber) {
            this.hkidNumber = hkidNumber;
            return this;
        }

        public Builder chineseName(ChineseName chineseName) {
            ChineseName value = chineseName != null ? chineseName : new ChineseName();
            this.chineseSurname = value.getSurname();
            this.chinesePersonalName = value.getPersonalName();
            this.chineseCommercialCodes = value.getCommercialCodes();
            return this;
        }

        public Builder chineseSurname(String chineseSurname) {
            this.chineseSurname = chineseSurname;
            return this;
        }

        public Builder chinesePersonalName(String chinesePersonalName) {
            this.chinesePersonalName = chinesePersonalName;
            return this;
        }

        public Builder chineseCommercialCodes(List<String> chineseCommercialCodes) {
            this.chineseCommercialCodes = chineseCommercialCodes;
            return this;
        }

        public Builder englishName(EnglishName englishName) {
            EnglishName value = englishName != null ? englishName : new EnglishName();
            this.englishSurname = value.getSurname();
            this.englishPersonalName = value.getPersonalName();
            return this;
        }

        public Builder englishSurname(String englishSurname) {
            this.englishSurname = englishSurname;
            return this;
        }

        public Builder englishPersonalName(String englishPersonalName) {
            this.englishPersonalName = englishPersonalName;
            return this;
        }

        public Builder sex(Sex sex) {
            this.sex = sex;
            return this;
        }

        public Builder sexEngMarker(String sexEngMarker) {
            this.sex = sexEngMarker != null ? Sex.fromEngMarker(sexEngMarker) : null;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder symbols(HkidSymbols symbols) {
            if (symbols == null) {
                throw new IllegalArgumentException("HKID symbols cannot be null");
            }
            this.symbols = symbols;
            return this;
        }

        public Builder symbolCodes(String symbolCodes) {
            return symbols(HkidSymbols.parse(symbolCodes));
        }

        public Builder firstRegistrationYearMonth(YearMonth firstRegistrationYearMonth) {
            this.firstRegistrationYearMonth = firstRegistrationYearMonth;
            return this;
        }

        public Builder dateOfRegistration(LocalDate dateOfRegistration) {
            this.dateOfRegistration = dateOfRegistration;
            return this;
        }

        public HkidCard build() {
            return new HkidCard(this);
        }
    }
}
