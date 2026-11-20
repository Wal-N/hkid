//! Validated collections of symbols printed on current smart HKID cards.

use std::{error::Error, fmt, str::FromStr};

use chrono::{Datelike, NaiveDate};

use crate::symbol::{HkidSymbol, HkidSymbolCategory};
use crate::text::{is_separator, trim_outer};

/// The reason a symbol collection could not be parsed or constructed.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ParseSymbolsError {
    /// A printed code could not be recognized at this character.
    UnknownCode(char),
    /// The collection contains the same symbol more than once.
    Duplicate(HkidSymbol),
    /// Two different symbols belong to the same exclusive category.
    Conflicting(HkidSymbol, HkidSymbol),
}

impl fmt::Display for ParseSymbolsError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::UnknownCode(ch) => write!(f, "unrecognized HKID symbol at {ch:?}"),
            Self::Duplicate(symbol) => write!(f, "duplicate HKID symbol {symbol}"),
            Self::Conflicting(first, second) => {
                write!(f, "conflicting HKID symbols {first} and {second}")
            }
        }
    }
}

impl Error for ParseSymbolsError {}

/// The reason a collection failed its age check.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum SymbolAgeError {
    /// The holder had not been born on the reference date.
    BirthAfterReferenceDate,
    /// The holder was outside the printed symbol's age range.
    Ineligible(HkidSymbol),
}

impl fmt::Display for SymbolAgeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::BirthAfterReferenceDate => f.write_str("birth date is after the reference date"),
            Self::Ineligible(symbol) => write!(
                f,
                "holder's age is outside the range for HKID symbol {symbol}"
            ),
        }
    }
}

impl Error for SymbolAgeError {}

/// An immutable collection of distinct, compatible symbols in canonical card order.
///
/// Parse printed codes with `str::parse`. Codes may be adjacent or separated by
/// spaces (ASCII, non-breaking or ideographic). The three stars in `***` must
/// be adjacent. Display joins the codes without spaces.
#[derive(Clone, Debug, Default, Eq, Hash, PartialEq)]
pub struct HkidSymbols(Vec<HkidSymbol>);

impl HkidSymbols {
    /// Constructs a collection from typed symbols, rejecting duplicates and conflicts.
    pub fn from_symbols(
        symbols: impl IntoIterator<Item = HkidSymbol>,
    ) -> Result<Self, ParseSymbolsError> {
        let mut result = Self::default();
        for symbol in symbols {
            result.insert(symbol)?;
        }
        Ok(result)
    }

    /// Tests a printed symbol string with the same rules as `str::parse`.
    pub fn is_valid(input: &str) -> bool {
        input.parse::<Self>().is_ok()
    }

    /// The symbols in canonical order, as printed on the card.
    pub fn as_slice(&self) -> &[HkidSymbol] {
        &self.0
    }

    /// Returns the members of one category in canonical order.
    pub fn by_category(
        &self,
        category: HkidSymbolCategory,
    ) -> impl Iterator<Item = HkidSymbol> + '_ {
        self.0
            .iter()
            .copied()
            .filter(move |symbol| symbol.category() == category)
    }

    /// Whether this collection contains the requested symbol.
    pub fn contains(&self, symbol: HkidSymbol) -> bool {
        self.0.contains(&symbol)
    }

    /// Whether the collection has no symbols.
    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    /// Checks the Re-entry Permit symbol against the holder's age on the reference date.
    /// For a card, supply its registration date. The adult symbol requires age 18 or over and
    /// the minor symbol requires age 11 through 17. A holder may have neither symbol.
    pub fn validate_age(
        &self,
        date_of_birth: NaiveDate,
        reference_date: NaiveDate,
    ) -> Result<(), SymbolAgeError> {
        if date_of_birth > reference_date {
            return Err(SymbolAgeError::BirthAfterReferenceDate);
        }
        // Comparing month/day puts a leap-day birthday's anniversary on March 1 in a common year.
        let before_birthday = (reference_date.month(), reference_date.day())
            < (date_of_birth.month(), date_of_birth.day());
        let age = reference_date.year() - date_of_birth.year() - i32::from(before_birthday);
        match self
            .by_category(HkidSymbolCategory::ReEntryPermitEligibility)
            .next()
        {
            Some(symbol @ HkidSymbol::AdultReEntryPermit) if age < 18 => {
                Err(SymbolAgeError::Ineligible(symbol))
            }
            Some(symbol @ HkidSymbol::MinorReEntryPermit) if !(11..18).contains(&age) => {
                Err(SymbolAgeError::Ineligible(symbol))
            }
            _ => Ok(()),
        }
    }

    fn insert(&mut self, symbol: HkidSymbol) -> Result<(), ParseSymbolsError> {
        let position = match self.0.binary_search(&symbol) {
            Ok(_) => return Err(ParseSymbolsError::Duplicate(symbol)),
            Err(index) => index,
        };
        let category = symbol.category();
        if !category.allows_multiple()
            && let Some(other) = self.by_category(category).next()
        {
            return Err(ParseSymbolsError::Conflicting(other, symbol));
        }
        self.0.insert(position, symbol);
        Ok(())
    }
}

impl FromStr for HkidSymbols {
    type Err = ParseSymbolsError;

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        let mut symbols = Self::default();
        for mut component in trim_outer(input).split(is_separator) {
            while let Some(first) = component.chars().next() {
                let (symbol, rest) = HkidSymbol::take_from(component)
                    .ok_or(ParseSymbolsError::UnknownCode(first))?;
                symbols.insert(symbol)?;
                component = rest;
            }
        }
        Ok(symbols)
    }
}

impl fmt::Display for HkidSymbols {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        for symbol in &self.0 {
            f.write_str(symbol.code())?;
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn typed_construction_and_lookup_follow_printed_order() {
        use HkidSymbol::{BirthDetailsChanged, NameChanged, RightOfAbode};

        let symbols =
            HkidSymbols::from_symbols([NameChanged, BirthDetailsChanged, RightOfAbode]).unwrap();
        assert_eq!(symbols, "ｎbＡ".parse().unwrap());
        assert_eq!(
            symbols.as_slice(),
            [RightOfAbode, BirthDetailsChanged, NameChanged]
        );
        assert_eq!(
            symbols
                .by_category(HkidSymbolCategory::OtherInformation)
                .collect::<Vec<_>>(),
            [BirthDetailsChanged, NameChanged]
        );
        assert!(symbols.contains(BirthDetailsChanged));
        assert!(!symbols.contains(HkidSymbol::BornElsewhere));
        assert!(HkidSymbols::from_symbols([]).unwrap().is_empty());
    }

    #[test]
    fn typed_construction_rejects_identical_and_conflicting_symbols() {
        use HkidSymbol::{NameChanged, RightOfAbode, RightToLand};

        assert_eq!(
            HkidSymbols::from_symbols([NameChanged, NameChanged]),
            Err(ParseSymbolsError::Duplicate(NameChanged))
        );
        assert_eq!(
            HkidSymbols::from_symbols([RightOfAbode, RightToLand]),
            Err(ParseSymbolsError::Conflicting(RightOfAbode, RightToLand))
        );
    }

    #[test]
    fn minor_symbol_expires_on_the_eighteenth_birthday() {
        let minor: HkidSymbols = "*".parse().unwrap();
        let birth = NaiveDate::from_ymd_opt(2004, 2, 29).unwrap();
        assert_eq!(
            minor.validate_age(birth, NaiveDate::from_ymd_opt(2022, 2, 28).unwrap()),
            Ok(())
        );
        assert_eq!(
            minor.validate_age(birth, NaiveDate::from_ymd_opt(2022, 3, 1).unwrap()),
            Err(SymbolAgeError::Ineligible(HkidSymbol::MinorReEntryPermit))
        );
    }
}
