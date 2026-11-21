use chrono::{Datelike, Days, Months, NaiveDate};

use super::{GenerationError, HkidGenerator};
use crate::card::completed_years;
use crate::{
    DefinedPrefix, HkidCard, HkidCardFields, HkidNumber, HkidSymbol, HkidSymbols, Sex, YearMonth,
};

const FIRST_ISSUE_DATE: NaiveDate = NaiveDate::from_ymd_opt(1949, 1, 1).unwrap();

impl HkidGenerator {
    /// Generates a complete sample card as of today in the system time zone.
    /// A requested sex also controls the pool of given-name characters.
    pub fn card(&mut self, sex: Option<Sex>) -> Result<HkidCard, GenerationError> {
        self.card_on(crate::holder::local_today(), sex)
    }

    /// Generates a complete sample card as of a date on or after 26 November 2018.
    /// The holder is 11 through 100 on that date. The current registration
    /// may fall on the reference date itself.
    pub fn card_on(
        &mut self,
        reference: NaiveDate,
        sex: Option<Sex>,
    ) -> Result<HkidCard, GenerationError> {
        if reference < HkidCard::SMART_CARD_START_DATE {
            return Err(GenerationError::ReferenceBeforeSmartCard);
        }
        let age = self.rng.u32(11..=100);
        let earliest_birth = reference - Months::new(12 * (age + 1)) + Days::new(1);
        let latest_birth = reference - Months::new(12 * age);
        let birth = self.inclusive_date(earliest_birth, latest_birth);
        self.card_for_birth(birth, reference, sex)
    }

    fn card_for_birth(
        &mut self,
        birth: NaiveDate,
        reference: NaiveDate,
        sex: Option<Sex>,
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
        let first = self.registration_month(eleventh_birthday.max(FIRST_ISSUE_DATE), registration);
        let number = self.card_number(birth, first, registration)?;
        let sex = sex.unwrap_or_else(|| {
            if self.rng.bool() {
                Sex::Male
            } else {
                Sex::Female
            }
        });
        let name = self.name(Some(sex), None)?;
        let symbols = HkidSymbols::from_symbols([
            age_symbol,
            HkidSymbol::RightOfAbode,
            HkidSymbol::BornInHongKong,
        ])
        .map_err(GenerationError::InvalidSymbols)?;
        HkidCard::new(HkidCardFields {
            hkid_number: Some(number),
            chinese_name: name.chinese,
            english_name: name.english,
            sex: Some(sex),
            date_of_birth: Some(birth),
            symbols,
            first_registration_year_month: Some(first),
            date_of_registration: Some(registration),
        })
        .map_err(GenerationError::InvalidCard)
    }

    fn inclusive_date(&mut self, first: NaiveDate, last: NaiveDate) -> NaiveDate {
        let days = (last - first).num_days() as u64;
        first + Days::new(self.rng.u64(0..=days))
    }

    fn registration_month(&mut self, first: NaiveDate, last: NaiveDate) -> YearMonth {
        let months = (last.year() - first.year()) * 12 + last.month() as i32 - first.month() as i32;
        let offset = self.rng.u32(0..=months as u32);
        (first.with_day(1).unwrap() + Months::new(offset)).into()
    }

    fn card_number(
        &mut self,
        birth: NaiveDate,
        first_registration: YearMonth,
        registration: NaiveDate,
    ) -> Result<HkidNumber, GenerationError> {
        // A Hong Kong birth registration can be recorded up to 42 days after birth.
        let birth_registration = birth + Days::new(self.rng.u64(0..=42));
        let prefix =
            if let Some(prefix) = DefinedPrefix::from_birth_registration_date(birth_registration) {
                prefix
            } else {
                let candidates = DefinedPrefix::from_first_issue_month_through(
                    first_registration.year(),
                    first_registration.month(),
                    registration,
                );
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

    fn date(year: i32, month: u32, day: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(year, month, day).unwrap()
    }

    #[test]
    fn registration_can_land_on_the_last_representable_date() {
        let mut generator = HkidGenerator::with_seed(42);
        let last = NaiveDate::MAX;
        let eighteenth_birthday = generator
            .card_for_birth(last - Months::new(18 * 12), last, None)
            .unwrap();
        assert_eq!(
            eighteenth_birthday.fields().date_of_registration,
            Some(last)
        );
        eighteenth_birthday.validate_as_of(last).unwrap();
    }

    #[test]
    fn leap_day_birthdays_transition_on_march_first() {
        let mut generator = HkidGenerator::with_seed(12);
        let birth = date(2004, 2, 29);
        let minor = generator
            .card_for_birth(birth, date(2022, 2, 28), Some(Sex::Female))
            .unwrap();
        assert!(
            minor
                .fields()
                .symbols
                .contains(HkidSymbol::MinorReEntryPermit)
        );
        assert_eq!(minor.fields().sex, Some(Sex::Female));
        let adult = generator
            .card_for_birth(birth, date(2022, 3, 1), Some(Sex::Male))
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
            .card_for_birth(date(2016, 2, 29), date(2027, 3, 1), None)
            .unwrap();
        assert_eq!(child.fields().date_of_registration, Some(date(2027, 3, 1)));
        assert_eq!(
            child.fields().first_registration_year_month,
            YearMonth::new(2027, 3)
        );
    }

    #[test]
    fn prefers_the_birth_registration_prefix_and_bounds_first_issue_by_the_printed_date() {
        let mut generator = HkidGenerator::with_seed(1);
        let july = YearMonth::new(2026, 7).unwrap();
        let first_birth_that_can_reach_y = date(1989, 1, 1) - Days::new(42);
        let mut birth_prefixes = std::collections::HashSet::new();
        let mut issue_prefixes = std::collections::HashSet::new();
        for _ in 0..512 {
            let number = generator
                .card_number(first_birth_that_can_reach_y, july, date(2026, 7, 27))
                .unwrap();
            birth_prefixes.insert(number.defined_prefix().unwrap().id());
            let previous_birth = first_birth_that_can_reach_y.pred_opt().unwrap();
            assert_eq!(
                generator
                    .card_number(previous_birth, july, date(2026, 7, 27))
                    .unwrap()
                    .prefix(),
                "Z"
            );
            let older_birth = date(1950, 1, 1);
            assert_eq!(
                generator
                    .card_number(older_birth, july, date(2026, 7, 26))
                    .unwrap()
                    .prefix(),
                "F"
            );
            let number = generator
                .card_number(older_birth, july, date(2026, 7, 27))
                .unwrap();
            issue_prefixes.insert(number.defined_prefix().unwrap().id());
        }
        assert_eq!(birth_prefixes, ["Y", "Z"].into());
        assert_eq!(issue_prefixes, ["F", "L"].into());
    }
}
