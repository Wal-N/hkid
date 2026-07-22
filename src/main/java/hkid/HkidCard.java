package hkid;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Represents the data printed on a Hong Kong Identity Card.
 */
public class HkidCard {
    static final LocalDate CURRENT_SMART_HKID_START_DATE = LocalDate.of(2018, 11, 26);

    private HkidNum hkidNum;
    private ChiName chiName = new ChiName();
    private EngName engName = new EngName();
    private Sex sex;
    private LocalDate dateOfBirth;
    private HkidSymbols symbols = HkidSymbols.empty();
    private YearMonth firstRegistrationYearMonth;
    private LocalDate dateOfRegistration;

    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FIRST_REGISTRATION_YEAR_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MM-yy");
    private static final DateTimeFormatter DOR_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");

    /**
     * Constructs an HkidCard instance.
     */
    public HkidCard() {
    }

    @Override
    public String toString() {
        return String.format("%s[hkidNum=%s, dateOfRegistration=%s]",
                getClass().getSimpleName(),
                getHkidNumMaskedStr(),
                getDateOfRegistrationStr());
    }

    public Optional<Integer> getAge() {
        return Optional.ofNullable(dateOfBirth).map(dob -> Period.between(dob, LocalDate.now()).getYears());
    }

    // Getters and setters

    public HkidNum getHkidNum() {
        return hkidNum;
    }

    public String getHkidNumStr() {
        return getHkidNumStr(HkidNum.Format.WithoutCheckDigit);
    }

    public String getHkidNumStr(HkidNum.Format format) {
        return hkidNum != null ? hkidNum.toString(format) : null;
    }

    public String getHkidNumMaskedStr() {
        return hkidNum != null ? hkidNum.getHMaskedStr() : null;
    }

    public void setHkidNum(HkidNum hkidNum) {
        this.hkidNum = hkidNum;
    }

    /**
     * Returns the printed Chinese name. Use {@link #getChiNameInfo()} when surname,
     * personal name, or commercial codes are needed separately.
     */
    public String getChiName() {
        return chiName.getFullName();
    }

    public ChiName getChiNameInfo() {
        return chiName;
    }

    public void setChiName(ChiName chiName) {
        this.chiName = chiName != null ? chiName : new ChiName();
    }

    public String getChiSurname() {
        return chiName.getSurname();
    }

    public void setChiSurname(String chiSurname) {
        chiName.setSurname(chiSurname);
    }

    public String getChiPersonalName() {
        return chiName.getPersonalName();
    }

    public void setChiPersonalName(String chiPersonalName) {
        chiName.setPersonalName(chiPersonalName);
    }

    public List<String> getChiCommercialCode() {
        return chiName.getCommercialCodes();
    }

    public void setChiCommercialCode(List<String> chiCommercialCode) {
        chiName.setCommercialCodes(chiCommercialCode);
    }

    /**
     * Returns the printed English name. Use {@link #getEngNameInfo()} when surname
     * and personal name are needed separately.
     */
    public String getEngName() {
        return engName.getFullName();
    }

    public EngName getEngNameInfo() {
        return engName;
    }

    public void setEngName(EngName engName) {
        this.engName = engName != null ? engName : new EngName();
    }

    public String getEngSurname() {
        return engName.getSurname();
    }

    public void setEngSurname(String engSurname) {
        engName.setSurname(engSurname);
    }

    public String getEngPersonalName() {
        return engName.getPersonalName();
    }

    public void setEngPersonalName(String engPersonalName) {
        engName.setPersonalName(engPersonalName);
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

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    /**
     * Convenience setter for parser/OCR input containing the English marker.
     */
    public void setSexEngMarker(String sexEngMarker) {
        this.sex = sexEngMarker != null ? Sex.fromEngMarker(sexEngMarker) : null;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDateOfBirthStr() {
        return dateOfBirth != null ? dateOfBirth.format(DOB_FORMATTER) : null;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        if (dateOfBirth != null && dateOfRegistration != null && dateOfRegistration.isBefore(dateOfBirth)) {
            throw new IllegalArgumentException("Date of registration cannot be before date of birth");
        }
        if (dateOfBirth != null && firstRegistrationYearMonth != null
                && firstRegistrationYearMonth.isBefore(YearMonth.from(dateOfBirth))) {
            throw new IllegalArgumentException("First registration month cannot be before date of birth");
        }
        validatePrintedAge(dateOfBirth, symbols, dateOfRegistration);
        this.dateOfBirth = dateOfBirth;
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

    public void setSymbols(HkidSymbols symbols) {
        if (symbols == null) {
            throw new IllegalArgumentException("HKID symbols cannot be null");
        }
        validatePrintedAge(dateOfBirth, symbols, dateOfRegistration);
        this.symbols = symbols;
    }

    public void setSymbolCodes(String symbolCodes) {
        setSymbols(HkidSymbols.parse(symbolCodes));
    }

    public YearMonth getFirstRegistrationYearMonth() {
        return firstRegistrationYearMonth;
    }

    public String getFirstRegistrationYearMonthStr() {
        return firstRegistrationYearMonth != null
                ? firstRegistrationYearMonth.format(FIRST_REGISTRATION_YEAR_MONTH_FORMATTER)
                : null;
    }

    public void setFirstRegistrationYearMonth(YearMonth firstRegistrationYearMonth) {
        if (firstRegistrationYearMonth != null && firstRegistrationYearMonth.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException("First registration month cannot be in the future");
        }
        if (firstRegistrationYearMonth != null && dateOfBirth != null
                && firstRegistrationYearMonth.isBefore(YearMonth.from(dateOfBirth))) {
            throw new IllegalArgumentException("First registration month cannot be before date of birth");
        }
        if (firstRegistrationYearMonth != null && dateOfRegistration != null
                && firstRegistrationYearMonth.isAfter(YearMonth.from(dateOfRegistration))) {
            throw new IllegalArgumentException("First registration month cannot be after date of registration");
        }
        this.firstRegistrationYearMonth = firstRegistrationYearMonth;
    }

    public LocalDate getDateOfRegistration() {
        return dateOfRegistration;
    }

    public String getDateOfRegistrationStr() {
        return dateOfRegistration != null ? dateOfRegistration.format(DOR_FORMATTER) : null;
    }

    public void setDateOfRegistration(LocalDate dateOfRegistration) {
        if (dateOfRegistration != null && dateOfRegistration.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of registration cannot be in the future");
        }
        if (dateOfRegistration != null && dateOfRegistration.isBefore(CURRENT_SMART_HKID_START_DATE)) {
            throw new IllegalArgumentException(
                    "Current smart HKID registration date cannot be before 26-11-2018");
        }
        if (dateOfRegistration != null && dateOfBirth != null && dateOfRegistration.isBefore(dateOfBirth)) {
            throw new IllegalArgumentException("Date of registration cannot be before date of birth");
        }
        if (dateOfRegistration != null && firstRegistrationYearMonth != null
                && firstRegistrationYearMonth.isAfter(YearMonth.from(dateOfRegistration))) {
            throw new IllegalArgumentException("Date of registration cannot be before first registration month");
        }
        validatePrintedAge(dateOfBirth, symbols, dateOfRegistration);
        this.dateOfRegistration = dateOfRegistration;
    }

    /**
     * Validates the card's dated fields and age-dependent symbols as of the
     * supplied date. Card-face consistency is always checked against the date of
     * registration; this method is intended for checking a historical card at a
     * later (or earlier) point in time.
     *
     * @param referenceDate date on which the card is being checked
     */
    public void validateAsOf(LocalDate referenceDate) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
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
        if (dateOfBirth != null) {
            symbols.validateAge(dateOfBirth, referenceDate);
        }
    }

    private static void validatePrintedAge(
            LocalDate dateOfBirth, HkidSymbols symbols, LocalDate dateOfRegistration) {
        if (dateOfBirth != null && dateOfRegistration != null) {
            symbols.validateAge(dateOfBirth, dateOfRegistration);
        }
    }

}
