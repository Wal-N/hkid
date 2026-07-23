package io.github.wal_n.hkid.number;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Predefined HKID number prefixes and their associated holder metadata.
 * Reference from L/M (82) in RP 32/230/R of Registration of Persons Offices and Wikipedia.
 */
public enum DefinedPrefix {
    A("Original ID cards, issued between 1949 and 1962, most holders were born before 1950",
            "首批身份證，1949-1962年間在簽發，大部份人在1950年代之前出生。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1949, 1, 1), LocalDate.of(1963, 1, 1)),
    B("Issued between 1955 and 1960 in city offices",
            "1955-1960年間在市區辦事處簽發。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1955, 1, 1), LocalDate.of(1961, 1, 1)),
    C("Issued between 1960 and 1983 in NT offices, if a child most born between 1946 and 1971, principally HK born",
            "1960-1983年間在新界辦事處簽發，如小童申請人多於1946-1971年間出生，以香港出生者為主。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1960, 1, 1), LocalDate.of(1984, 1, 1)),
    D("Issued between 1960 and 1983 at HK Island office, if a child most born between, principally HK born",
            "1960-1983年間在港島辦事處簽發，如小童申請人多於1946-1971年間出生，以香港出生者為主。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1960, 1, 1), LocalDate.of(1984, 1, 1)),
    E("Issued between 1955 and 1969 in Kowloon offices, if a child most born between 1946 and 1962, principally HK born",
            "1955-1969年間在九龍辦事處簽發，如小童申請人多於1946-1962年間出生，以香港出生者為主。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1955, 1, 1), LocalDate.of(1970, 1, 1)),
    F("First issue of a card commencing from 24 February 2020",
            "2020年2月24日起首次獲簽發身份證的人士。",
            DateBasis.FIRST_ISSUE, LocalDate.of(2020, 2, 24), null),
    G("Issued between 1967 and 1983 in Kowloon offices, if a child most born between 1956 and 1971",
            "1967-1983年間在九龍辦事處簽發，如小童申請人多於1956-1971年間出生。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1967, 1, 1), LocalDate.of(1984, 1, 1)),
    H("Issued between 1979 and 1983 in HK Island offices, if a child most born between 1968 and 1971, principally HK born",
            "1979-1983年間在港島辦事處簽發，如小童申請人多於1968-1971年間出生，以香港以外出生者為主。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1979, 1, 1), LocalDate.of(1984, 1, 1)),
    J("Consular officers after 23 October 1991",
            "1991年10月23日開始簽發予領事館僱員。"),
    K("First issue of an ID card between 28 March 1983 and 31 July 1990, if a child most born between 1972 and 1979",
            "1983年3月28日至1990年7月31日首次獲簽發身份證的人士，如小童申請人多於1972年至1979年6月在香港出生。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1983, 3, 28), LocalDate.of(1990, 8, 1)),
    L("Issued between 1983 and 2003, used when computer system malfunctioned",
            "1983-2003年間簽發，電腦系統故障時使用的備用號碼。2003年6月23日起停用。"),
    M("First issue of ID card between 1 August 2011 and 23 February 2020",
            "2011年8月1日至2020年2月23日首次獲簽發身份證的人士，如小童申請人多於2000年起在香港以外出生。",
            DateBasis.FIRST_ISSUE, LocalDate.of(2011, 8, 1), LocalDate.of(2020, 2, 24)),
    N("Birth registered in Hong Kong after 1 June 2019",
            "2019年6月1日起於香港登記出生的人士。",
            DateBasis.BIRTH_REGISTRATION, LocalDate.of(2019, 6, 1), null),
    P("First issue of an ID card between 1 August 1990 and 27 December 2000, if a child most born between July and December 1979",
            "1990年8月1日至2000年12月27日首次獲簽發身份證的人士，如小童申請人多於1979年7月至12月在香港出生，或1980年代在香港以外出生。",
            DateBasis.FIRST_ISSUE, LocalDate.of(1990, 8, 1), LocalDate.of(2000, 12, 28)),
    R("First issue of an ID card between 28 December 2000 and 31 July 2011",
            "2000年12月28日至2011年7月31日首次獲簽發身份證的人士，以香港以外出生者為主。",
            DateBasis.FIRST_ISSUE, LocalDate.of(2000, 12, 28), LocalDate.of(2011, 8, 1)),
    S("Birth registered in Hong Kong between 1 April 2005 and 31 May 2019",
            "2005年4月1日至2019年5月31日於香港登記出生的人士。",
            DateBasis.BIRTH_REGISTRATION, LocalDate.of(2005, 4, 1), LocalDate.of(2019, 6, 1)),
    T("Issued between 1983 and 1997, used when computer system malfunctioned",
            "1983-1997年間簽發，電腦系統故障時使用的備用號碼。1997年7月1日起停用。"),
    V("Child under 11 issued with a \"Document of Identity for Visa Purposes\" between 28 March 1983 and 31 August 2003",
            "1983年3月28日至2003年8月31日獲簽發簽證身份書的11歲以下兒童。"),
    W("First issue to a foreign labourer or foreign domestic helper between 10 November 1989 and 1 January 2009",
            "1989年11月10日至2009年1月1日首次獲簽發身份證的外籍勞工及外籍家庭傭工。"),
    Y("Birth registered in Hong Kong between 1 January 1989 and 31 March 2005",
            "1989年1月1日至2005年3月31日於香港登記出生的人士。",
            DateBasis.BIRTH_REGISTRATION, LocalDate.of(1989, 1, 1), LocalDate.of(2005, 4, 1)),
    Z("Birth registered in Hong Kong between 1 January 1980 and 31 December 1988",
            "1980年1月1日至1988年12月31日於香港登記出生的人士。",
            DateBasis.BIRTH_REGISTRATION, LocalDate.of(1980, 1, 1), LocalDate.of(1989, 1, 1)),
    WX("First issue to a foreign labourer or foreign domestic helper since 2 January 2009",
            "2009年1月2日起首次獲簽發身份證的外籍勞工及外籍家庭傭工。"),
    XA("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
    XB("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
    XC("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
    XD("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
    XE("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
    XG("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
    XH("ID card issues to person without a Chinese name before 27 March 1983",
            "1983年3月27日前沒有中文姓名的新登記身份證人士。");
    // U: Pseudo ID for neonatal born in public hospitals

    private enum DateBasis {
        BIRTH_REGISTRATION,
        FIRST_ISSUE
    }

    private final String description;
    private final String traditionalChineseDescription;
    private final LocalDate hongKongBirthRegistrationStartDate;
    private final LocalDate hongKongBirthRegistrationEndDateExclusive;
    private final LocalDate firstIssueStartDate;
    private final LocalDate firstIssueEndDateExclusive;

    DefinedPrefix(String description, String traditionalChineseDescription) {
        this.description = description;
        this.traditionalChineseDescription = traditionalChineseDescription;
        this.hongKongBirthRegistrationStartDate = null;
        this.hongKongBirthRegistrationEndDateExclusive = null;
        this.firstIssueStartDate = null;
        this.firstIssueEndDateExclusive = null;
    }

    DefinedPrefix(String description,
                  String traditionalChineseDescription,
                  DateBasis dateBasis,
                  LocalDate startDate,
                  LocalDate endDateExclusive) {
        if (dateBasis == null || startDate == null) {
            throw new IllegalArgumentException("Dated prefix metadata requires a basis and start date");
        }
        if (endDateExclusive != null && !endDateExclusive.isAfter(startDate)) {
            throw new IllegalArgumentException("Prefix metadata end date must be after start date");
        }
        this.description = description;
        this.traditionalChineseDescription = traditionalChineseDescription;
        this.hongKongBirthRegistrationStartDate = dateBasis == DateBasis.BIRTH_REGISTRATION
                ? startDate : null;
        this.hongKongBirthRegistrationEndDateExclusive = dateBasis == DateBasis.BIRTH_REGISTRATION
                ? endDateExclusive : null;
        this.firstIssueStartDate = dateBasis == DateBasis.FIRST_ISSUE ? startDate : null;
        this.firstIssueEndDateExclusive = dateBasis == DateBasis.FIRST_ISSUE ? endDateExclusive : null;
    }

    public String getDescription() {
        return description;
    }

    public String getTraditionalChineseDescription() {
        return traditionalChineseDescription;
    }

    /**
     * Returns whether this prefix has an exact Hong Kong birth-registration period
     * containing the supplied date. Approximate historical birth ranges are not
     * treated as exact metadata by this method.
     */
    public boolean supportsHongKongBirthRegistrationDate(LocalDate birthRegistrationDate) {
        return birthRegistrationDate != null
                && hongKongBirthRegistrationStartDate != null
                && !birthRegistrationDate.isBefore(hongKongBirthRegistrationStartDate)
                && (hongKongBirthRegistrationEndDateExclusive == null
                || birthRegistrationDate.isBefore(hongKongBirthRegistrationEndDateExclusive));
    }

    /**
     * Finds the prefix whose exact Hong Kong birth-registration period contains
     * the supplied date.
     */
    public static Optional<DefinedPrefix> fromHongKongBirthRegistrationDate(
            LocalDate birthRegistrationDate) {
        for (DefinedPrefix definedPrefix : values()) {
            if (definedPrefix.supportsHongKongBirthRegistrationDate(birthRegistrationDate)) {
                return Optional.of(definedPrefix);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether at least one day in the supplied month falls within this
     * prefix's exact first-issue period.
     */
    public boolean supportsFirstIssueMonth(YearMonth firstIssueMonth) {
        return firstIssueMonth != null
                && firstIssueStartDate != null
                && !firstIssueMonth.atEndOfMonth().isBefore(firstIssueStartDate)
                && (firstIssueEndDateExclusive == null
                || firstIssueMonth.atDay(1).isBefore(firstIssueEndDateExclusive));
    }

    /**
     * Finds all standard prefixes whose exact first-issue period overlaps the
     * supplied month.
     */
    public static DefinedPrefix[] fromFirstIssueMonth(YearMonth firstIssueMonth) {
        return Arrays.stream(values())
                .filter(prefix -> prefix.supportsFirstIssueMonth(firstIssueMonth))
                .toArray(DefinedPrefix[]::new);
    }

    /**
     * Looks up predefined metadata for a prefix.
     *
     * @param prefix A one- or two-letter HKID prefix.
     * @return The matching predefined prefix, or an empty optional when no metadata is defined.
     */
    public static Optional<DefinedPrefix> fromPrefix(String prefix) {
        if (prefix == null) {
            return Optional.empty();
        }

        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT);
        for (DefinedPrefix definedPrefix : values()) {
            if (definedPrefix.name().equals(normalizedPrefix)) {
                return Optional.of(definedPrefix);
            }
        }
        return Optional.empty();
    }
}
