//! Random test data with caller-owned generator state.

mod cards;
mod names;

pub use names::{ChineseNameEntry, GeneratedName};

use std::fmt;

use chrono::{NaiveDate, TimeDelta};

use crate::{CardError, DefinedPrefix, HkidNumber, ParseSymbolsError};

/// A requested sample could not be generated from the arguments or bundled data.
#[derive(Clone, Debug, Eq, PartialEq)]
pub enum GenerationError {
    EmptyPrefixes,
    InvalidDateRange,
    ReferenceBeforeSmartCard,
    InvalidNameLength,
    InvalidSeed(String),
    InsufficientNameEntries,
    NoCompatiblePrefix,
    InvalidSymbols(ParseSymbolsError),
    InvalidCard(CardError),
}

impl fmt::Display for GenerationError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::EmptyPrefixes => f.write_str("at least one allowed prefix is required"),
            Self::InvalidDateRange => f.write_str("the start date must precede the end date"),
            Self::ReferenceBeforeSmartCard => {
                f.write_str("the reference date must be on or after 2018-11-26")
            }
            Self::InvalidNameLength => f.write_str("personal name length must be between 1 and 5"),
            Self::InvalidSeed(reason) => write!(f, "invalid name seed: {reason}"),
            Self::InsufficientNameEntries => f.write_str("insufficient eligible name seed entries"),
            Self::NoCompatiblePrefix => f.write_str("no defined prefix fits the generated dates"),
            Self::InvalidSymbols(error) => write!(f, "invalid generated symbols: {error}"),
            Self::InvalidCard(error) => write!(f, "invalid generated card: {error}"),
        }
    }
}

impl std::error::Error for GenerationError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::InvalidSymbols(error) => Some(error),
            Self::InvalidCard(error) => Some(error),
            _ => None,
        }
    }
}

/// Generates sample HKID data. A seeded instance can replay a sequence of calls.
pub struct HkidGenerator {
    rng: fastrand::Rng,
}

impl Default for HkidGenerator {
    fn default() -> Self {
        Self::new()
    }
}

impl HkidGenerator {
    /// Creates a generator with a fresh seed.
    pub fn new() -> Self {
        Self {
            rng: fastrand::Rng::new(),
        }
    }

    /// Creates a generator for repeatable samples with the same build, reference
    /// data, explicit dates, and sequence of calls.
    pub fn with_seed(seed: u64) -> Self {
        Self {
            rng: fastrand::Rng::with_seed(seed),
        }
    }

    /// Generates a number using a prefix from the bundled reference table.
    pub fn number(&mut self) -> HkidNumber {
        let prefixes = DefinedPrefix::all();
        let prefix = &prefixes[self.rng.usize(..prefixes.len())];
        self.number_for_prefix(prefix.id())
    }

    /// Generates a number with one or two uppercase ASCII letters as its prefix.
    pub fn number_with_any_prefix(&mut self) -> HkidNumber {
        let length = self.rng.usize(1..=2);
        let prefix: String = (0..length).map(|_| self.rng.char('A'..='Z')).collect();
        self.number_for_prefix(&prefix)
    }

    /// Chooses from the supplied defined prefixes. An empty slice is an error.
    pub fn number_from_prefixes(
        &mut self,
        allowed: &[&DefinedPrefix],
    ) -> Result<HkidNumber, GenerationError> {
        let prefix = self
            .rng
            .choice(allowed)
            .ok_or(GenerationError::EmptyPrefixes)?;
        Ok(self.number_for_prefix(prefix.id()))
    }

    fn number_for_prefix(&mut self, prefix: &str) -> HkidNumber {
        let numerals = format!("{:06}", self.rng.u32(..1_000_000));
        HkidNumber::from_parts(prefix, &numerals, None).expect("generated valid number components")
    }

    /// Samples a date from the inclusive start up to the exclusive end.
    /// Passing `None` uses today's date in the system time zone at this call.
    pub fn date_in_range(
        &mut self,
        start: NaiveDate,
        end: Option<NaiveDate>,
    ) -> Result<NaiveDate, GenerationError> {
        let days = (end.unwrap_or_else(crate::holder::local_today) - start).num_days();
        if days <= 0 {
            return Err(GenerationError::InvalidDateRange);
        }
        Ok(start + TimeDelta::days(self.rng.i64(0..days)))
    }
}
