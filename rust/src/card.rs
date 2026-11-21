//! Complete or partial smart HKID cards and validation of their dated fields.

use std::{error::Error, fmt, str::FromStr};

use chrono::{Datelike, NaiveDate};

use crate::{ChineseName, EnglishName, HkidNumber, HkidSymbol, HkidSymbols, SymbolAgeError};

/// The sex marker on the card.
#[derive(Clone, Copy, Debug, Eq, Hash, PartialEq)]
pub enum Sex {
    Male,
    Female,
}

impl Sex {
    /// The single English letter printed on the card.
    pub fn english_marker(self) -> &'static str {
        match self {
            Self::Male => "M",
            Self::Female => "F",
        }
    }

    /// The Chinese character printed beside the English marker.
    pub fn chinese_marker(self) -> &'static str {
        match self {
            Self::Male => "男",
            Self::Female => "女",
        }
    }
}

impl fmt::Display for Sex {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} {}", self.chinese_marker(), self.english_marker())
    }
}

impl FromStr for Sex {
    type Err = CardError;

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        // The Java marker parser trims characters through ASCII space before matching.
        match input.trim_matches(|ch| ch <= ' ') {
            "M" | "m" => Ok(Self::Male),
            "F" | "f" => Ok(Self::Female),
            _ => Err(CardError::InvalidSex),
        }
    }
}

/// A calendar month in the range supported by [`NaiveDate`]. Parses and displays as ISO year-month.
#[derive(Clone, Copy, Debug, Eq, Hash, Ord, PartialEq, PartialOrd)]
pub struct YearMonth(NaiveDate);

impl YearMonth {
    /// Constructs a calendar month from its year and one-based month number.
    pub fn new(year: i32, month: u32) -> Option<Self> {
        NaiveDate::from_ymd_opt(year, month, 1).map(Self)
    }

    pub fn year(self) -> i32 {
        self.0.year()
    }

    pub fn month(self) -> u32 {
        self.0.month()
    }
}

impl From<NaiveDate> for YearMonth {
    fn from(date: NaiveDate) -> Self {
        Self(date.with_day(1).expect("every month has a first day"))
    }
}

impl FromStr for YearMonth {
    type Err = CardError;

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        let (year, month) = input.rsplit_once('-').ok_or(CardError::InvalidYearMonth)?;
        let year = year.parse().map_err(|_| CardError::InvalidYearMonth)?;
        let month = month.parse().map_err(|_| CardError::InvalidYearMonth)?;
        let value = Self::new(year, month).ok_or(CardError::InvalidYearMonth)?;
        if value.to_string() != input {
            return Err(CardError::InvalidYearMonth);
        }
        Ok(value)
    }
}

impl fmt::Display for YearMonth {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0.format("%Y-%m"))
    }
}

/// A field could not be parsed, or its value conflicts with another date or symbol.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum CardError {
    InvalidSex,
    InvalidYearMonth,
    RegistrationBeforeSmartCardPeriod,
    RegistrationBeforeBirth,
    FirstRegistrationBeforeBirthMonth,
    FirstRegistrationAfterRegistrationMonth,
    IneligibleSymbol(HkidSymbol),
    BirthAfterReferenceDate,
    RegistrationAfterReferenceDate,
    FirstRegistrationAfterReferenceMonth,
}

impl fmt::Display for CardError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::InvalidSex => "expected an English sex marker M or F",
            Self::InvalidYearMonth => "expected an ISO calendar year and month",
            Self::RegistrationBeforeSmartCardPeriod => "registration precedes 2018-11-26",
            Self::RegistrationBeforeBirth => "registration precedes birth",
            Self::FirstRegistrationBeforeBirthMonth => {
                "first registration precedes the birth month"
            }
            Self::FirstRegistrationAfterRegistrationMonth => {
                "first registration is after the current-card registration month"
            }
            Self::IneligibleSymbol(symbol) => {
                return write!(
                    f,
                    "holder's age at registration is outside the range for {symbol}"
                );
            }
            Self::BirthAfterReferenceDate => "birth is after the reference date",
            Self::RegistrationAfterReferenceDate => "registration is after the reference date",
            Self::FirstRegistrationAfterReferenceMonth => {
                "first registration is after the reference month"
            }
        })
    }
}

impl Error for CardError {}

/// Fields to assemble before constructing an [`HkidCard`]. The default represents an empty card.
#[derive(Clone, Default, Eq, Hash, PartialEq)]
pub struct HkidCardFields {
    pub hkid_number: Option<HkidNumber>,
    pub chinese_name: ChineseName,
    pub english_name: EnglishName,
    pub sex: Option<Sex>,
    pub date_of_birth: Option<NaiveDate>,
    pub symbols: HkidSymbols,
    pub first_registration_year_month: Option<YearMonth>,
    pub date_of_registration: Option<NaiveDate>,
}

impl fmt::Debug for HkidCardFields {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("HkidCardFields")
            .field(
                "hkid_number",
                &self.hkid_number.as_ref().map(HkidNumber::masked),
            )
            .field("date_of_registration", &self.date_of_registration)
            .finish_non_exhaustive()
    }
}

/// An immutable card whose supplied fields have been checked for consistency.
#[derive(Clone, Default, Eq, Hash, PartialEq)]
pub struct HkidCard(HkidCardFields);

impl HkidCard {
    /// First day of registration for current smart HKID cards.
    pub const SMART_CARD_START_DATE: NaiveDate = NaiveDate::from_ymd_opt(2018, 11, 26).unwrap();

    /// Checks date ordering and age-dependent symbols on the current-card registration date.
    /// Each cross-field check runs when its input fields are present.
    pub fn new(fields: HkidCardFields) -> Result<Self, CardError> {
        if let Some(registration) = fields.date_of_registration {
            if registration < Self::SMART_CARD_START_DATE {
                return Err(CardError::RegistrationBeforeSmartCardPeriod);
            }
            if let Some(birth) = fields.date_of_birth {
                fields
                    .symbols
                    .validate_age(birth, registration)
                    .map_err(|error| match error {
                        SymbolAgeError::BirthAfterReferenceDate => {
                            CardError::RegistrationBeforeBirth
                        }
                        SymbolAgeError::Ineligible(symbol) => CardError::IneligibleSymbol(symbol),
                    })?;
            }
        }
        if let Some(first) = fields.first_registration_year_month {
            if fields.date_of_birth.is_some_and(|date| first < date.into()) {
                return Err(CardError::FirstRegistrationBeforeBirthMonth);
            }
            if fields
                .date_of_registration
                .is_some_and(|date| first > date.into())
            {
                return Err(CardError::FirstRegistrationAfterRegistrationMonth);
            }
        }
        Ok(Self(fields))
    }

    /// Borrows the validated card fields.
    pub fn fields(&self) -> &HkidCardFields {
        &self.0
    }

    /// Returns the fields by value, for example to change a field and rebuild the card.
    pub fn into_fields(self) -> HkidCardFields {
        self.0
    }

    /// Age in completed years on the given date. A missing birth date yields `Ok(None)`.
    pub fn age_on(&self, reference: NaiveDate) -> Result<Option<i32>, CardError> {
        let Some(birth) = self.0.date_of_birth else {
            return Ok(None);
        };
        if birth > reference {
            return Err(CardError::BirthAfterReferenceDate);
        }
        Ok(Some(completed_years(birth, reference)))
    }

    /// Checks each supplied date against the reference day and first registration against its month.
    pub fn validate_as_of(&self, reference: NaiveDate) -> Result<(), CardError> {
        if self.0.date_of_birth.is_some_and(|date| date > reference) {
            return Err(CardError::BirthAfterReferenceDate);
        }
        if self
            .0
            .date_of_registration
            .is_some_and(|date| date > reference)
        {
            return Err(CardError::RegistrationAfterReferenceDate);
        }
        if self
            .0
            .first_registration_year_month
            .is_some_and(|month| month > reference.into())
        {
            return Err(CardError::FirstRegistrationAfterReferenceMonth);
        }
        Ok(())
    }

    /// Printed birth date with a zero-padded year of era. Expanded years carry a leading plus.
    pub fn printed_date_of_birth(&self) -> Option<String> {
        self.0.date_of_birth.map(|date| {
            let year = date.year_ce().1;
            let sign = if year > 9999 { "+" } else { "" };
            format!("{:02}-{:02}-{sign}{year:04}", date.day(), date.month())
        })
    }

    /// Printed first-registration month and two-digit year.
    pub fn printed_first_registration_year_month(&self) -> Option<String> {
        self.0
            .first_registration_year_month
            .map(|month| format!("{:02}-{:02}", month.month(), month.0.year_ce().1 % 100))
    }

    /// Printed current-card registration day, month and two-digit year.
    pub fn printed_date_of_registration(&self) -> Option<String> {
        self.0
            .date_of_registration
            .map(|date| date.format("%d-%m-%y").to_string())
    }
}

/// The diagnostic representation includes the masked number and printed registration date.
impl fmt::Display for HkidCard {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let masked_number = self.0.hkid_number.as_ref().map(HkidNumber::masked);
        let registration = self.printed_date_of_registration();
        write!(
            f,
            "HkidCard[hkidNumber={}, dateOfRegistration={}]",
            masked_number.as_deref().unwrap_or("null"),
            registration.as_deref().unwrap_or("null")
        )
    }
}

impl fmt::Debug for HkidCard {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fmt::Display::fmt(self, f)
    }
}

/// Completed years for chronologically ordered dates. Comparing the month and day makes a
/// leap-day birthday's anniversary 1 March in a common year.
pub(crate) fn completed_years(birth: NaiveDate, reference: NaiveDate) -> i32 {
    let before_birthday = (reference.month(), reference.day()) < (birth.month(), birth.day());
    reference.year() - birth.year() - i32::from(before_birthday)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_card_has_no_printed_dates_or_age() {
        let card = HkidCard::default();
        let reference = HkidCard::SMART_CARD_START_DATE;
        assert_eq!(card.age_on(reference), Ok(None));
        assert_eq!(card.validate_as_of(reference), Ok(()));
        assert_eq!(card.printed_date_of_birth(), None);
        assert_eq!(card.printed_first_registration_year_month(), None);
        assert_eq!(card.printed_date_of_registration(), None);
    }

    #[test]
    fn partial_cards_check_registration_dates_independently() {
        let start = HkidCard::SMART_CARD_START_DATE;
        let card = HkidCard::new(HkidCardFields {
            date_of_registration: Some(start),
            symbols: "***".parse().unwrap(),
            ..Default::default()
        })
        .unwrap();
        assert_eq!(card.validate_as_of(start), Ok(()));
        assert_eq!(
            card.validate_as_of(start.pred_opt().unwrap()),
            Err(CardError::RegistrationAfterReferenceDate)
        );

        let card = HkidCard::new(HkidCardFields {
            first_registration_year_month: Some(YearMonth::from(start)),
            ..Default::default()
        })
        .unwrap();
        assert_eq!(card.validate_as_of("2018-11-01".parse().unwrap()), Ok(()));
        assert_eq!(
            card.validate_as_of("2018-10-31".parse().unwrap()),
            Err(CardError::FirstRegistrationAfterReferenceMonth)
        );
    }

    #[test]
    fn year_month_uses_iso_calendar_precision() {
        assert!(YearMonth::new(2020, 13).is_none());
        assert!("2020-2".parse::<YearMonth>().is_err());
        assert!("2020-02-01".parse::<YearMonth>().is_err());
        for date in [
            NaiveDate::MIN,
            NaiveDate::MAX,
            HkidCard::SMART_CARD_START_DATE,
        ] {
            let month = YearMonth::from(date);
            assert_eq!(month.to_string().parse::<YearMonth>(), Ok(month));
        }
        let birth = "2020-02-29".parse().unwrap();
        let card = HkidCard::new(HkidCardFields {
            date_of_birth: Some(birth),
            first_registration_year_month: Some("2020-02".parse().unwrap()),
            date_of_registration: Some(birth),
            ..Default::default()
        })
        .unwrap();
        assert_eq!(card.validate_as_of(birth), Ok(()));
        assert_eq!(
            card.age_on(birth.pred_opt().unwrap()),
            Err(CardError::BirthAfterReferenceDate)
        );
    }

    #[test]
    fn card_face_dates_print_the_year_of_era() {
        for (iso_month, printed_birth, printed_month) in [
            ("0000-01", "03-01-0001", "01-01"),
            ("-0001-01", "03-01-0002", "01-02"),
            ("+10000-01", "03-01-+10000", "01-00"),
            ("-9999-01", "03-01-+10000", "01-00"),
        ] {
            let month = iso_month.parse::<YearMonth>().unwrap();
            let card = HkidCard::new(HkidCardFields {
                date_of_birth: Some(format!("{iso_month}-03").parse().unwrap()),
                first_registration_year_month: Some(month),
                ..Default::default()
            })
            .unwrap();
            assert_eq!(month.to_string(), iso_month);
            assert_eq!(card.printed_date_of_birth().as_deref(), Some(printed_birth));
            assert_eq!(
                card.printed_first_registration_year_month().as_deref(),
                Some(printed_month)
            );
        }
    }

    #[test]
    fn sex_accepts_the_english_card_marker_in_either_case() {
        assert_eq!(" f\t".parse(), Ok(Sex::Female));
        assert_eq!(Sex::Female.chinese_marker(), "女");
        assert_eq!(Sex::Male.english_marker(), "M");
        for value in ["", "Ｆ", "女 F", " X "] {
            assert_eq!(value.parse::<Sex>(), Err(CardError::InvalidSex));
        }
    }

    #[test]
    fn card_display_and_debug_print_only_the_masked_number_and_registration() {
        let fields = HkidCardFields {
            hkid_number: Some("A123456".parse().unwrap()),
            english_name: EnglishName::new(Some("Chan"), None).unwrap(),
            date_of_registration: Some(HkidCard::SMART_CARD_START_DATE),
            ..Default::default()
        };
        let card = HkidCard::new(fields).unwrap();
        assert_eq!(
            card.to_string(),
            "HkidCard[hkidNumber=****456(*), dateOfRegistration=26-11-18]"
        );
        assert_eq!(format!("{card:?}"), card.to_string());
    }
}
