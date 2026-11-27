use std::{fmt, ops::RangeInclusive};

use chrono::{Datelike, Days, Months, NaiveDate};

use super::{GenerationError, HkidGenerator};
use crate::card::completed_years;
use crate::{
    DefinedPrefix, HkidCard, HkidCardFields, HkidNumber, HkidSymbol, HkidSymbolCategory,
    HkidSymbols, Sex,
};

const FIRST_ISSUE_DATE: NaiveDate = NaiveDate::from_ymd_opt(1949, 1, 1).unwrap();

/// Age and birthplace constraints for a randomly generated card.
#[derive(Clone, Debug)]
pub struct CardGenerationOptions {
    ages: RangeInclusive<i32>,
    birthplace: Option<HkidSymbol>,
}

impl Default for CardGenerationOptions {
    fn default() -> Self {
        Self {
            ages: 11..=85,
            birthplace: None,
        }
    }
}

/// An invalid age range or birthplace constraint.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum InvalidCardOptions {
    AgeRange,
    Birthplace,
}

impl fmt::Display for InvalidCardOptions {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::AgeRange => "ages must satisfy 11 <= min_age <= max_age <= 100",
            Self::Birthplace => "expected a reported place-of-birth symbol",
        })
    }
}

impl std::error::Error for InvalidCardOptions {}

impl CardGenerationOptions {
    /// Use an inclusive age range within 11 to 100. An absent birthplace uses census weights.
    pub fn new(
        ages: RangeInclusive<i32>,
        birthplace: Option<HkidSymbol>,
    ) -> Result<Self, InvalidCardOptions> {
        if ages.is_empty() || *ages.start() < 11 || *ages.end() > 100 {
            return Err(InvalidCardOptions::AgeRange);
        }
        if birthplace.is_some_and(|s| s.category() != HkidSymbolCategory::ReportedPlaceOfBirth) {
            return Err(InvalidCardOptions::Birthplace);
        }
        Ok(Self { ages, birthplace })
    }
}

impl HkidGenerator {
    /// Generates a complete sample card with a random sex as of today in the system time zone.
    /// Uses the same age range and birthplace distribution as [`Self::card_on`].
    pub fn card(&mut self) -> Result<HkidCard, GenerationError> {
        self.card_on(crate::holder::local_today(), None)
    }

    /// Generates a complete sample card as of today in the system time zone.
    /// Uses the same age range and birthplace distribution as [`Self::card_on`].
    /// The specified sex also controls the pool of given-name characters.
    pub fn card_with_sex(&mut self, sex: Sex) -> Result<HkidCard, GenerationError> {
        self.card_on(crate::holder::local_today(), Some(sex))
    }

    /// Generates a complete sample card as of a date on or after 26 November 2018.
    /// Ages 11 through 85 are equally likely on that date. Birthplace is sampled
    /// independently using the all-ages distribution from Hong Kong's 2021 Population Census.
    /// The current registration may fall on the reference date itself.
    pub fn card_on(
        &mut self,
        reference: NaiveDate,
        sex: Option<Sex>,
    ) -> Result<HkidCard, GenerationError> {
        self.card_with_options(reference, sex, CardGenerationOptions::default())
    }

    /// Generates a card with the requested age range and birthplace as of the reference date.
    pub fn card_with_options(
        &mut self,
        reference: NaiveDate,
        sex: Option<Sex>,
        options: CardGenerationOptions,
    ) -> Result<HkidCard, GenerationError> {
        if reference < HkidCard::SMART_CARD_START_DATE {
            return Err(GenerationError::ReferenceBeforeSmartCard);
        }
        let age = self.rng.i32(options.ages) as u32;
        let earliest_birth = reference - Months::new(12 * (age + 1)) + Days::new(1);
        let latest_birth = reference - Months::new(12 * age);
        let birth = self.inclusive_date(earliest_birth, latest_birth);
        let birthplace = options
            .birthplace
            .unwrap_or_else(|| self.census_birthplace());
        self.card_for_birth(birth, reference, sex, birthplace)
    }

    fn card_for_birth(
        &mut self,
        birth: NaiveDate,
        reference: NaiveDate,
        sex: Option<Sex>,
        birthplace: HkidSymbol,
    ) -> Result<HkidCard, GenerationError> {
        let eleventh_birthday = anniversary(birth, 11);
        let (age_symbol, eligible_from) = if completed_years(birth, reference) >= 18 {
            (HkidSymbol::AdultReEntryPermit, anniversary(birth, 18))
        } else {
            (HkidSymbol::MinorReEntryPermit, eleventh_birthday)
        };
        let earliest_registration = (reference - Months::new(120))
            .max(HkidCard::SMART_CARD_START_DATE)
            .max(eligible_from);
        let registration = self.inclusive_date(earliest_registration, reference);
        let first = self.first_eligible_day_in_random_month(
            eleventh_birthday.max(FIRST_ISSUE_DATE),
            registration,
        );
        // A Hong Kong birth registration can be recorded up to 42 days after birth.
        let hk_birth_registration = (birthplace == HkidSymbol::BornInHongKong)
            .then(|| birth + Days::new(self.rng.u64(0..=42)));
        let number = self.card_number(hk_birth_registration, first, registration)?;
        let sex = sex.unwrap_or_else(|| {
            if self.rng.bool() {
                Sex::Male
            } else {
                Sex::Female
            }
        });
        let name = self.name(Some(sex), None)?;
        let symbols = HkidSymbols::from_symbols([age_symbol, HkidSymbol::RightOfAbode, birthplace])
            .map_err(GenerationError::InvalidSymbols)?;
        HkidCard::new(HkidCardFields {
            hkid_number: Some(number),
            chinese_name: name.chinese,
            english_name: name.english,
            sex: Some(sex),
            date_of_birth: Some(birth),
            symbols,
            first_registration_year_month: Some(first.into()),
            date_of_registration: Some(registration),
        })
        .map_err(GenerationError::InvalidCard)
    }

    fn inclusive_date(&mut self, first: NaiveDate, last: NaiveDate) -> NaiveDate {
        let days = (last - first).num_days() as u64;
        first + Days::new(self.rng.u64(0..=days))
    }

    fn first_eligible_day_in_random_month(
        &mut self,
        first: NaiveDate,
        last: NaiveDate,
    ) -> NaiveDate {
        let months = (last.year() - first.year()) * 12 + last.month() as i32 - first.month() as i32;
        let offset = self.rng.u32(0..=months as u32);
        (first.with_day(1).unwrap() + Months::new(offset)).max(first)
    }

    fn card_number(
        &mut self,
        hk_birth_registration: Option<NaiveDate>,
        first_eligible_day: NaiveDate,
        registration: NaiveDate,
    ) -> Result<HkidNumber, GenerationError> {
        let prefix = if let Some(prefix) =
            hk_birth_registration.and_then(DefinedPrefix::from_birth_registration_date)
        {
            prefix
        } else {
            let candidates =
                DefinedPrefix::first_issue_prefixes_from(first_eligible_day, Some(registration));
            self.rng
                .choice(candidates)
                .ok_or(GenerationError::NoCompatiblePrefix)?
        };
        Ok(self.number_for_prefix(prefix.id()))
    }
}

fn anniversary(birth: NaiveDate, years: u32) -> NaiveDate {
    let anniversary = birth + Months::new(12 * years);
    // Adding whole years clamps 29 February to 28 February in a common year.
    // The legal anniversary for age-specific card symbols is 1 March.
    if anniversary.day() != birth.day() {
        anniversary + Days::new(1)
    } else {
        anniversary
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::YearMonth;

    fn date(year: i32, month: u32, day: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(year, month, day).unwrap()
    }

    #[test]
    fn registration_can_land_on_the_last_representable_date() {
        let mut generator = HkidGenerator::new();
        let last = NaiveDate::MAX;
        let eighteenth_birthday = generator
            .card_for_birth(
                last - Months::new(18 * 12),
                last,
                None,
                HkidSymbol::BornElsewhere,
            )
            .unwrap();
        assert_eq!(
            eighteenth_birthday.fields().date_of_registration,
            Some(last)
        );
        eighteenth_birthday.validate_as_of(last).unwrap();
    }

    #[test]
    fn leap_day_birthdays_transition_on_march_first() {
        let mut generator = HkidGenerator::new();
        let birth = date(2004, 2, 29);
        let minor = generator
            .card_for_birth(
                birth,
                date(2022, 2, 28),
                Some(Sex::Female),
                HkidSymbol::BornInHongKong,
            )
            .unwrap();
        assert!(
            minor
                .fields()
                .symbols
                .contains(HkidSymbol::MinorReEntryPermit)
        );
        assert_eq!(minor.fields().sex, Some(Sex::Female));
        let adult = generator
            .card_for_birth(
                birth,
                date(2022, 3, 1),
                Some(Sex::Male),
                HkidSymbol::BornInHongKong,
            )
            .unwrap();
        assert!(
            adult
                .fields()
                .symbols
                .contains(HkidSymbol::AdultReEntryPermit)
        );
        assert_eq!(adult.fields().date_of_registration, Some(date(2022, 3, 1)));
        assert_eq!(adult.fields().sex, Some(Sex::Male));
        let child = generator
            .card_for_birth(
                date(2016, 2, 29),
                date(2027, 3, 1),
                None,
                HkidSymbol::BornInHongKong,
            )
            .unwrap();
        assert_eq!(child.fields().date_of_registration, Some(date(2027, 3, 1)));
        assert_eq!(
            child.fields().first_registration_year_month,
            YearMonth::new(2027, 3)
        );
    }

    #[test]
    fn birthplace_controls_the_card_symbol_and_prefix_on_the_eleventh_birthday() {
        let mut generator = HkidGenerator::new();
        let birth = date(2015, 7, 27);
        let registration = date(2026, 7, 27);
        for (birthplace, expected_prefix) in [
            (HkidSymbol::BornInHongKong, "S"),
            (HkidSymbol::BornInMainland, "L"),
            (HkidSymbol::BornInMacao, "L"),
            (HkidSymbol::BornElsewhere, "L"),
        ] {
            let card = generator
                .card_for_birth(birth, registration, None, birthplace)
                .unwrap();
            assert!(card.fields().symbols.contains(birthplace));
            assert_eq!(
                card.fields().hkid_number.as_ref().unwrap().prefix(),
                expected_prefix
            );
            assert!(!card.is_suspicious());
        }
    }

    #[test]
    fn first_issue_prefixes_respect_the_birthday_within_the_registration_month() {
        let mut generator = HkidGenerator::new();
        let registration = date(2026, 7, 27);
        for (birthday, expected) in [(26, vec!["F", "L"]), (27, vec!["L"])] {
            let first = generator.first_eligible_day_in_random_month(
                anniversary(date(2015, 7, birthday), 11),
                registration,
            );
            let candidates = DefinedPrefix::first_issue_prefixes_from(first, Some(registration));
            assert_eq!(
                candidates
                    .into_iter()
                    .map(DefinedPrefix::id)
                    .collect::<Vec<_>>(),
                expected
            );
        }
    }

    #[test]
    fn prefers_the_birth_registration_prefix_and_bounds_first_issue_by_the_printed_date() {
        let mut generator = HkidGenerator::new();
        let july = date(2026, 7, 1);
        for (birth_registration, expected) in [(date(1988, 12, 31), "Z"), (date(1989, 1, 1), "Y")] {
            assert_eq!(
                generator
                    .card_number(Some(birth_registration), july, date(2026, 7, 27))
                    .unwrap()
                    .prefix(),
                expected
            );
        }
        let older_birth_registration = Some(date(1950, 1, 1));
        let before = generator
            .card_number(older_birth_registration, july, date(2026, 7, 26))
            .unwrap();
        assert_eq!(before.prefix(), "F");
        let on_transition = generator
            .card_number(older_birth_registration, july, date(2026, 7, 27))
            .unwrap();
        assert!(matches!(on_transition.prefix(), "F" | "L"));
    }
}
