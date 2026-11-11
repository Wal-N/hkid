package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.ChineseCommercialCode;
import io.github.wal_n.hkid.number.HkidNumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("java.util.Locale.default")
class HkidHolderSummaryTest {
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 7, 21);
    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");

    @Test
    void describesCompleteCardInBothLanguagesAndStableSentenceOrder() {
        HkidCard card = HkidCard.builder()
                .sex(Sex.FEMALE)
                .dateOfBirth(LocalDate.of(2008, 7, 21))
                .symbolCodes("NBZ***A")
                .build();

        assertSummary(card,
                "持有人為18歲女性，於香港出生。"
                        + "持有人享有香港居留權，於領證登記時年滿18歲，並符合回港證申領資格。"
                        + "持有人的出生日期或出生地曾更改，姓名亦曾更改。",
                "The holder is female, aged 18, and was born in Hong Kong. "
                        + "The holder has the right of abode in Hong Kong "
                        + "and, at registration, was aged 18 or over "
                        + "and eligible for a Re-entry Permit. "
                        + "The holder's date or place of birth has been amended, as has their name.");
    }

    @Test
    void describesAdultHongKongBornFemaleInTwoSentences() {
        HkidCard card = HkidCard.builder()
                .sex(Sex.FEMALE)
                .dateOfBirth(LocalDate.of(1975, 7, 21))
                .symbolCodes("***AZ")
                .build();

        assertSummary(card,
                "持有人為51歲女性，於香港出生。"
                        + "持有人享有香港居留權，於領證登記時年滿18歲，並符合回港證申領資格。",
                "The holder is female, aged 51, and was born in Hong Kong. "
                        + "The holder has the right of abode in Hong Kong "
                        + "and, at registration, was aged 18 or over "
                        + "and eligible for a Re-entry Permit.");
    }

    @Test
    void omitsMissingDemographicFieldsNaturally() {
        assertSummary(HkidCard.builder().build(), "", "");
        assertSummary(HkidCard.builder().sex(Sex.MALE).build(),
                "持有人為男性。", "The holder is male.");
        assertSummary(HkidCard.builder().sex(Sex.FEMALE).build(),
                "持有人為女性。", "The holder is female.");
        assertSummary(HkidCard.builder().dateOfBirth(LocalDate.of(2008, 7, 21)).build(),
                "持有人現年18歲。", "The holder is 18 years old.");
        assertSummary(HkidCard.builder()
                        .sex(Sex.MALE)
                        .dateOfBirth(LocalDate.of(2008, 7, 21))
                        .build(),
                "持有人為18歲男性。", "The holder is male, aged 18.");
    }

    @Test
    void joinsBirthplaceWithOnlyTheAvailableDemographics() {
        assertSummary(HkidCard.builder().sex(Sex.FEMALE).symbolCodes("X").build(),
                "持有人為女性，於內地出生。",
                "The holder is female and was born in the Chinese Mainland.");
        assertSummary(HkidCard.builder()
                        .dateOfBirth(LocalDate.of(2008, 7, 21))
                        .symbolCodes("W")
                        .build(),
                "持有人現年18歲，於澳門出生。",
                "The holder is 18 years old and was born in Macao.");
        assertSummary(HkidCard.builder().sex(Sex.MALE).symbolCodes("O").build(),
                "持有人為男性，於其他地區出生。",
                "The holder is male and was born elsewhere.");
        assertSummary(HkidCard.builder()
                        .sex(Sex.MALE)
                        .dateOfBirth(LocalDate.of(2025, 7, 21))
                        .symbolCodes("O")
                        .build(),
                "持有人為1歲男性，於其他地區出生。",
                "The holder is male, aged 1, and was born elsewhere.");
    }

    @ParameterizedTest
    @CsvSource({
            "2026-07-21, 2026-07-21, 0, years",
            "2025-07-21, 2026-07-21, 1, year",
            "2008-07-21, 2026-07-20, 17, years",
            "2008-07-21, 2026-07-21, 18, years",
            "2008-07-21, 2026-07-22, 18, years",
            "2004-02-29, 2022-02-28, 17, years",
            "2004-02-29, 2022-03-01, 18, years",
            "2004-02-29, 2024-02-28, 19, years",
            "2004-02-29, 2024-02-29, 20, years"
    })
    void calculatesAgeOnTheReferenceDateIncludingLeapDayAnniversaries(
            LocalDate birthDate, LocalDate referenceDate, int age, String yearWord) {
        HkidCard card = HkidCard.builder().dateOfBirth(birthDate).build();

        assertSummary(card, referenceDate,
                "持有人現年" + age + "歲。",
                "The holder is " + age + " " + yearWord + " old.");
    }

    @ParameterizedTest
    @MethodSource("symbolSummaries")
    void describesEverySupportedSymbolWithoutInferringMissingFields(
            HkidSymbol symbol, String chinese, String english) {
        HkidCard card = HkidCard.builder().symbols(HkidSymbols.of(symbol)).build();

        assertSummary(card, chinese, english);
    }

    private static Stream<Arguments> symbolSummaries() {
        return Stream.of(
                Arguments.of(HkidSymbol.ADULT_RE_ENTRY_PERMIT,
                        "持有人於領證登記時年滿18歲，並符合回港證申領資格。",
                        "At registration, the holder was aged 18 or over "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of(HkidSymbol.MINOR_RE_ENTRY_PERMIT,
                        "持有人於領證登記時年齡為11至17歲，並符合回港證申領資格。",
                        "At registration, the holder was aged 11 to 17 "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of(HkidSymbol.RIGHT_OF_ABODE,
                        "持有人享有香港居留權。",
                        "The holder has the right of abode in Hong Kong."),
                Arguments.of(HkidSymbol.STAY_LIMITED,
                        "持有人於領證登記時有逗留限制。",
                        "At registration, the holder had a stay restriction."),
                Arguments.of(HkidSymbol.RIGHT_TO_LAND,
                        "持有人享有香港入境權。",
                        "The holder has the right to land in Hong Kong."),
                Arguments.of(HkidSymbol.STAY_NOT_LIMITED,
                        "持有人於領證登記時沒有逗留限制。",
                        "At registration, the holder had no stay restriction."),
                Arguments.of(HkidSymbol.BORN_IN_HONG_KONG,
                        "持有人於香港出生。",
                        "The holder was born in Hong Kong."),
                Arguments.of(HkidSymbol.BORN_IN_MAINLAND,
                        "持有人於內地出生。",
                        "The holder was born in the Chinese Mainland."),
                Arguments.of(HkidSymbol.BORN_IN_MACAO,
                        "持有人於澳門出生。",
                        "The holder was born in Macao."),
                Arguments.of(HkidSymbol.BORN_ELSEWHERE,
                        "持有人於其他地區出生。",
                        "The holder was born elsewhere."),
                Arguments.of(HkidSymbol.BIRTH_DETAILS_CHANGED,
                        "持有人的出生日期或出生地曾更改。",
                        "The holder's date or place of birth has been amended."),
                Arguments.of(HkidSymbol.NAME_CHANGED,
                        "持有人的姓名曾更改。",
                        "The holder's name has been amended."));
    }

    @Test
    void keepsHistoricalMinorEligibilitySeparateFromCurrentAdultAge() {
        HkidCard card = HkidCard.builder()
                .dateOfBirth(LocalDate.of(2008, 1, 1))
                .dateOfRegistration(LocalDate.of(2020, 6, 1))
                .symbolCodes("*")
                .build();

        assertSummary(card,
                "持有人現年18歲。"
                        + "持有人於領證登記時年齡為11至17歲，並符合回港證申領資格。",
                "The holder is 18 years old. "
                        + "At registration, the holder was aged 11 to 17 "
                        + "and eligible for a Re-entry Permit.");
        assertEquals("*", card.getSymbolsString());
    }

    @ParameterizedTest
    @MethodSource("statusAndEligibilitySummaries")
    void combinesResidentialStatusAndEligibilityWithTheCorrectTimeScope(
            String symbols, String chinese, String english) {
        assertSummary(HkidCard.builder().symbolCodes(symbols).build(), chinese, english);
    }

    private static Stream<Arguments> statusAndEligibilitySummaries() {
        return Stream.of(
                Arguments.of("***C",
                        "持有人於領證登記時有逗留限制，年滿18歲，並符合回港證申領資格。",
                        "At registration, the holder had a stay restriction and was aged 18 or over "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of("*C",
                        "持有人於領證登記時有逗留限制，年齡為11至17歲，並符合回港證申領資格。",
                        "At registration, the holder had a stay restriction and was aged 11 to 17 "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of("***U",
                        "持有人於領證登記時沒有逗留限制，年滿18歲，並符合回港證申領資格。",
                        "At registration, the holder had no stay restriction and was aged 18 or over "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of("*U",
                        "持有人於領證登記時沒有逗留限制，年齡為11至17歲，並符合回港證申領資格。",
                        "At registration, the holder had no stay restriction and was aged 11 to 17 "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of("*A",
                        "持有人享有香港居留權，於領證登記時年齡為11至17歲，並符合回港證申領資格。",
                        "The holder has the right of abode in Hong Kong "
                                + "and, at registration, was aged 11 to 17 "
                                + "and eligible for a Re-entry Permit."),
                Arguments.of("***R",
                        "持有人享有香港入境權，於領證登記時年滿18歲，並符合回港證申領資格。",
                        "The holder has the right to land in Hong Kong "
                                + "and, at registration, was aged 18 or over "
                                + "and eligible for a Re-entry Permit."));
    }

    @Test
    void omitsMissingSentenceGroupsAndKeepsBothAmendmentsUnambiguous() {
        assertSummary(HkidCard.builder().symbolCodes("BN").build(),
                "持有人的出生日期或出生地曾更改，姓名亦曾更改。",
                "The holder's date or place of birth has been amended, as has their name.");
        assertSummary(HkidCard.builder().sex(Sex.MALE).symbolCodes("N").build(),
                "持有人為男性。持有人的姓名曾更改。",
                "The holder is male. The holder's name has been amended.");
        assertSummary(HkidCard.builder().symbolCodes("ZAB").build(),
                "持有人於香港出生。持有人享有香港居留權。持有人的出生日期或出生地曾更改。",
                "The holder was born in Hong Kong. The holder has the right of abode in Hong Kong. "
                        + "The holder's date or place of birth has been amended.");
        assertSummary(HkidCard.builder()
                        .dateOfBirth(LocalDate.of(2008, 1, 1))
                        .dateOfRegistration(LocalDate.of(2020, 6, 1))
                        .symbolCodes("*UN")
                        .build(),
                "持有人現年18歲。"
                        + "持有人於領證登記時沒有逗留限制，年齡為11至17歲，並符合回港證申領資格。"
                        + "持有人的姓名曾更改。",
                "The holder is 18 years old. "
                        + "At registration, the holder had no stay restriction and was aged 11 to 17 "
                        + "and eligible for a Re-entry Permit. The holder's name has been amended.");
    }

    @Test
    void excludesIdentityAndRegistrationFieldsEvenWhenRegistrationIsInTheFuture() {
        HkidCard summaryFields = HkidCard.builder()
                .sex(Sex.FEMALE)
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .symbolCodes("***AZBN")
                .build();
        HkidCard completeCard = summaryFields.toBuilder()
                .chineseSurname("陳")
                .chinesePersonalName("大文")
                .chineseCommercialCodes(Arrays.asList(
                        new ChineseCommercialCode("7115"),
                        new ChineseCommercialCode("1129"),
                        new ChineseCommercialCode("2429")))
                .englishSurname("Chan")
                .englishPersonalName("Tai Man")
                .hkidNumber(new HkidNumber("Z123456"))
                .firstRegistrationYearMonth(YearMonth.of(2030, 1))
                .dateOfRegistration(LocalDate.of(2030, 6, 1))
                .build();

        for (Locale locale : Arrays.asList(Locale.ENGLISH, Locale.TRADITIONAL_CHINESE)) {
            String expected = summaryFields.describeHolder(locale, REFERENCE_DATE);
            assertEquals(expected, completeCard.describeHolder(locale, REFERENCE_DATE));
            assertEquals(expected,
                    HkidCardUtil.describeHolder(completeCard, locale, REFERENCE_DATE));
        }
        assertSummary(HkidCard.builder()
                        .hkidNumber(new HkidNumber("Z123456"))
                        .chineseSurname("陳")
                        .englishSurname("Chan")
                        .dateOfRegistration(LocalDate.of(2030, 6, 1))
                        .build(),
                "", "");
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh", "zh-HK", "zh-TW", "zh-Hant", "zh-CN", "zh-Hans"})
    void usesTraditionalChineseForChineseLocaleVariants(String languageTag) {
        HkidCard card = HkidCard.builder().sex(Sex.FEMALE).build();
        Locale locale = Locale.forLanguageTag(languageTag);

        assertEquals("持有人為女性。", card.describeHolder(locale, REFERENCE_DATE));
        assertEquals("持有人為女性。",
                HkidCardUtil.describeHolder(card, locale, REFERENCE_DATE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "en-HK", "en-GB", "en-US", "fr", "ja", "und"})
    void usesEnglishForEnglishAndUnsupportedLocales(String languageTag) {
        HkidCard card = HkidCard.builder().sex(Sex.FEMALE).build();
        Locale locale = Locale.forLanguageTag(languageTag);

        assertEquals("The holder is female.", card.describeHolder(locale, REFERENCE_DATE));
        assertEquals("The holder is female.",
                HkidCardUtil.describeHolder(card, locale, REFERENCE_DATE));
    }

    @Test
    void readsJvmDefaultLocaleOnEveryCallAndLetsExplicitLocaleOverrideIt() {
        Locale original = Locale.getDefault();
        Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
        HkidCard card = HkidCard.builder().sex(Sex.FEMALE).build();
        try {
            Locale.setDefault(Locale.TRADITIONAL_CHINESE);
            assertDefaultSummary(card, "持有人為女性。");
            assertEquals("The holder is female.", card.describeHolder(Locale.ENGLISH));
            assertEquals("The holder is female.", HkidCardUtil.describeHolder(card, Locale.ENGLISH));

            Locale.setDefault(Locale.ENGLISH);
            assertDefaultSummary(card, "The holder is female.");
            assertEquals("持有人為女性。", card.describeHolder(Locale.TRADITIONAL_CHINESE));
            assertEquals("持有人為女性。",
                    HkidCardUtil.describeHolder(card, Locale.TRADITIONAL_CHINESE));

            Locale.setDefault(Locale.FRENCH);
            assertDefaultSummary(card, "The holder is female.");
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT, originalFormat);
        }
    }

    private static void assertDefaultSummary(HkidCard card, String expected) {
        assertEquals(expected, card.describeHolder());
        assertEquals(expected, card.describeHolder(null));
        assertEquals(expected, card.describeHolder(null, REFERENCE_DATE));
        assertEquals(expected, HkidCardUtil.describeHolder(card));
        assertEquals(expected, HkidCardUtil.describeHolder(card, null));
        assertEquals(expected, HkidCardUtil.describeHolder(card, null, REFERENCE_DATE));
    }

    @Test
    @ResourceLock("java.util.TimeZone.default")
    void convenienceApisUseTodaysDateInHongKongRegardlessOfJvmTimeZone() {
        TimeZone original = TimeZone.getDefault();
        Locale originalLocale = Locale.getDefault();
        Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
        try {
            LocalDate hongKongDate = LocalDate.now(HONG_KONG);
            TimeZone.setDefault(TimeZone.getTimeZone("GMT-12:00"));
            if (LocalDate.now().equals(hongKongDate)) {
                TimeZone.setDefault(TimeZone.getTimeZone("GMT+14:00"));
            }
            Locale.setDefault(Locale.ENGLISH);
            LocalDate birthday = LocalDate.now().isBefore(hongKongDate)
                    ? hongKongDate : hongKongDate.plusDays(1);
            HkidCard card = HkidCard.builder()
                    .dateOfBirth(birthday.minusYears(20))
                    .build();

            LocalDate before = LocalDate.now(HONG_KONG);
            String[] summaries = {
                    card.describeHolder(),
                    card.describeHolder(Locale.ENGLISH),
                    card.describeHolder(null),
                    HkidCardUtil.describeHolder(card),
                    HkidCardUtil.describeHolder(card, Locale.ENGLISH),
                    HkidCardUtil.describeHolder(card, null)
            };
            LocalDate after = LocalDate.now(HONG_KONG);
            String expectedBefore = card.describeHolder(Locale.ENGLISH, before);
            String expectedAfter = card.describeHolder(Locale.ENGLISH, after);
            for (String summary : summaries) {
                assertTrue(summary.equals(expectedBefore) || summary.equals(expectedAfter), summary);
            }
        } finally {
            TimeZone.setDefault(original);
            Locale.setDefault(originalLocale);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT, originalFormat);
        }
    }

    @Test
    void rejectsMissingRequiredArgumentsAndFutureBirthDates() {
        HkidCard empty = HkidCard.builder().build();
        HkidCard futureBirth = HkidCard.builder()
                .dateOfBirth(REFERENCE_DATE.plusDays(1))
                .build();

        assertThrows(IllegalArgumentException.class, () -> HkidCardUtil.describeHolder(null));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.describeHolder(null, Locale.ENGLISH));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.describeHolder(null, Locale.ENGLISH, REFERENCE_DATE));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.describeHolder(empty, Locale.ENGLISH, null));
        assertThrows(IllegalArgumentException.class,
                () -> empty.describeHolder(Locale.ENGLISH, null));
        assertThrows(IllegalArgumentException.class,
                () -> futureBirth.describeHolder(Locale.ENGLISH, REFERENCE_DATE));
        assertThrows(IllegalArgumentException.class,
                () -> HkidCardUtil.describeHolder(futureBirth, Locale.ENGLISH, REFERENCE_DATE));
    }

    private static void assertSummary(HkidCard card, String chinese, String english) {
        assertSummary(card, REFERENCE_DATE, chinese, english);
    }

    private static void assertSummary(
            HkidCard card, LocalDate referenceDate, String chinese, String english) {
        assertEquals(chinese, card.describeHolder(Locale.TRADITIONAL_CHINESE, referenceDate));
        assertEquals(chinese,
                HkidCardUtil.describeHolder(card, Locale.TRADITIONAL_CHINESE, referenceDate));
        assertEquals(english, card.describeHolder(Locale.ENGLISH, referenceDate));
        assertEquals(english, HkidCardUtil.describeHolder(card, Locale.ENGLISH, referenceDate));
    }
}
