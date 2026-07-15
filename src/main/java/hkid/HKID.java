package hkid;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents the data printed on a Hong Kong Identity Card.
 */
public class HKID {
    private HkidNum hkidNum;
    private ChiName chiName;
    private EngName engName;
    private Sex sex;
    private LocalDate dateOfBirth;
    private List<DefinedSymbol> symbols = new ArrayList<>();
    private YearMonth dateOfIssue;
    private LocalDate dateOfRegistration;

    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DOI_FORMATTER = DateTimeFormatter.ofPattern("MM-yy");
    private static final DateTimeFormatter DOR_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");

    /**
     * Constructs an HKID instance.
     */
    public HKID() {
    }

    @Override
    public String toString() {
        return String.format("%s[hkidNum=%s, chiName=%s, engName=%s, chiCommercialCode=%s, sex=%s, dateOfBirth=%s, symbols=%s, dateOfIssue=%s, dateOfRegistration=%s]",
                getClass().getSimpleName(),
                getHkidNumStr(HkidNum.Format.Complete),
                getChiName(),
                getEngName(),
                getChiCommercialCode(),
                getSex(),
                getDateOfBirthStr(),
                getSymbols(),
                getDateOfIssueStr(),
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

    public void setHkidNum(HkidNum hkidNum) {
        this.hkidNum = hkidNum;
    }

    /**
     * Returns the printed Chinese name. Use {@link #getChiNameInfo()} when surname,
     * personal name, or commercial codes are needed separately.
     */
    public String getChiName() {
        return getChiFullName();
    }

    public ChiName getChiNameInfo() {
        return chiName;
    }

    public String getChiFullName() {
        return chiName != null ? chiName.getFullName() : null;
    }

    public void setChiName(ChiName chiName) {
        this.chiName = chiName;
    }

    public String getChiSurname() {
        return chiName != null ? chiName.getSurname() : null;
    }

    public void setChiSurname(String chiSurname) {
        ensureChiName().setSurname(chiSurname);
    }

    public String getChiPersonalName() {
        return chiName != null ? chiName.getPersonalName() : null;
    }

    public void setChiPersonalName(String chiPersonalName) {
        ensureChiName().setPersonalName(chiPersonalName);
    }

    public List<String> getChiCommercialCode() {
        return chiName != null ? chiName.getCommercialCodes() : Collections.emptyList();
    }

    public void setChiCommercialCode(List<String> chiCommercialCode) {
        ensureChiName().setCommercialCodes(chiCommercialCode);
    }

    /**
     * Returns the printed English name. Use {@link #getEngNameInfo()} when surname
     * and personal name are needed separately.
     */
    public String getEngName() {
        return getEngFullName();
    }

    public EngName getEngNameInfo() {
        return engName;
    }

    public String getEngFullName() {
        return engName != null ? engName.getFullName() : null;
    }

    public void setEngName(EngName engName) {
        this.engName = engName;
    }

    public String getEngSurname() {
        return engName != null ? engName.getSurname() : null;
    }

    public void setEngSurname(String engSurname) {
        ensureEngName().setSurname(engSurname);
    }

    public String getEngPersonalName() {
        return engName != null ? engName.getPersonalName() : null;
    }

    public void setEngPersonalName(String engPersonalName) {
        ensureEngName().setPersonalName(engPersonalName);
    }

    public Sex getSex() {
        return sex;
    }

    /**
     * Returns the HKID card sex marker, currently "M" or "F".
     */
    public String getSexCode() {
        return sex != null ? sex.getCode() : null;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    /**
     * Convenience setter for parser/OCR input where the card value is read as text.
     */
    public void setSexCode(String sexCode) {
        this.sex = sexCode != null ? Sex.fromCode(sexCode) : null;
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
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * HKID cards can display multiple symbols, for example "***AZ".
     */
    public List<DefinedSymbol> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    public void setSymbols(List<DefinedSymbol> symbols) {
        this.symbols = symbols == null
                ? new ArrayList<DefinedSymbol>()
                : new ArrayList<DefinedSymbol>(symbols);
    }

    public void addSymbol(DefinedSymbol symbol) {
        if (symbol != null) {
            symbols.add(symbol);
        }
    }

    public DefinedSymbol getSymbol() {
        return symbols.isEmpty() ? null : symbols.get(0);
    }

    public void setSymbol(DefinedSymbol symbol) {
        symbols.clear();
        addSymbol(symbol);
    }

    public YearMonth getDateOfIssue() {
        return dateOfIssue;
    }

    public String getDateOfIssueStr() {
        return dateOfIssue != null ? dateOfIssue.format(DOI_FORMATTER) : null;
    }

    public void setDateOfIssue(YearMonth dateOfIssue) {
        if (dateOfIssue != null && dateOfIssue.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException("Date of issue cannot be in the future");
        }
        if (dateOfIssue != null && dateOfRegistration != null
                && dateOfIssue.isBefore(YearMonth.from(dateOfRegistration))) {
            throw new IllegalArgumentException("Date of issue cannot be before date of registration");
        }
        this.dateOfIssue = dateOfIssue;
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
        if (dateOfRegistration != null && dateOfBirth != null && dateOfRegistration.isBefore(dateOfBirth)) {
            throw new IllegalArgumentException("Date of registration cannot be before date of birth");
        }
        if (dateOfRegistration != null && dateOfIssue != null
                && dateOfIssue.isBefore(YearMonth.from(dateOfRegistration))) {
            throw new IllegalArgumentException("Date of issue cannot be before date of registration");
        }
        this.dateOfRegistration = dateOfRegistration;
    }

    private ChiName ensureChiName() {
        if (chiName == null) {
            chiName = new ChiName();
        }
        return chiName;
    }

    private EngName ensureEngName() {
        if (engName == null) {
            engName = new EngName();
        }
        return engName;
    }
}
