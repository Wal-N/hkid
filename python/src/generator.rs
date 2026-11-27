use hkid_core::{GenerationError, HkidGenerator, NaiveDate, generator::CardGenerationOptions};
use pyo3::prelude::*;

use crate::card::{Card, SexInput};
use crate::name::GeneratedName;
use crate::number::{Number, Prefix};
use crate::symbols::SymbolInput;
use crate::value_error;

/// Generate sample numbers, names, dates and complete HKID cards.
#[pyclass(name = "HkidGenerator", module = "hkid_utils")]
pub(crate) struct Generator(HkidGenerator);

#[pymethods]
impl Generator {
    #[new]
    fn new() -> Self {
        Self(HkidGenerator::new())
    }

    /// Generate a number using a defined prefix.
    fn number(&mut self) -> Number {
        Number(self.0.number())
    }

    /// Generate a number with any one- or two-letter prefix.
    fn number_with_any_prefix(&mut self) -> Number {
        Number(self.0.number_with_any_prefix())
    }

    /// Choose a prefix from a nonempty sequence of DefinedPrefix values.
    fn number_from_prefixes(&mut self, prefixes: Vec<Prefix>) -> PyResult<Number> {
        let allowed: Vec<_> = prefixes.into_iter().map(|prefix| prefix.0).collect();
        self.0
            .number_from_prefixes(&allowed)
            .map(Number)
            .map_err(value_error)
    }

    /// Sample a date from start (inclusive) to end (exclusive). The default end is today.
    #[pyo3(signature = (start, end=None))]
    fn date_in_range(
        &mut self,
        py: Python<'_>,
        start: NaiveDate,
        end: Option<NaiveDate>,
    ) -> PyResult<NaiveDate> {
        let end = match end {
            Some(end) => end,
            None => crate::today(py)?,
        };
        self.0.date_in_range(start, Some(end)).map_err(value_error)
    }

    /// Generate a Chinese name with commercial codes and matching Cantonese romanisation.
    #[pyo3(signature = (*, sex=None, personal_name_length=None))]
    fn name(
        &mut self,
        sex: Option<SexInput<'_>>,
        personal_name_length: Option<i64>,
    ) -> PyResult<GeneratedName> {
        let length = personal_name_length
            .map(usize::try_from)
            .transpose()
            .map_err(|_| value_error(GenerationError::InvalidNameLength))?;
        self.0
            .name(sex.map(SexInput::into_sex).transpose()?, length)
            .map(GeneratedName)
            .map_err(value_error)
    }

    /// Generate a card valid today. Omit birthplace to use census weights.
    #[pyo3(signature = (*, sex=None, min_age=11, max_age=85, birthplace=None))]
    fn card(
        &mut self,
        py: Python<'_>,
        sex: Option<SexInput<'_>>,
        min_age: i32,
        max_age: i32,
        birthplace: Option<SymbolInput<'_>>,
    ) -> PyResult<Card> {
        self.card_on(crate::today(py)?, sex, min_age, max_age, birthplace)
    }

    /// Generate a card valid on the supplied date. Age bounds are inclusive.
    #[pyo3(signature = (reference_date, *, sex=None, min_age=11, max_age=85, birthplace=None))]
    fn card_on(
        &mut self,
        reference_date: NaiveDate,
        sex: Option<SexInput<'_>>,
        min_age: i32,
        max_age: i32,
        birthplace: Option<SymbolInput<'_>>,
    ) -> PyResult<Card> {
        let options = CardGenerationOptions::new(
            min_age..=max_age,
            birthplace.map(SymbolInput::into_symbol).transpose()?,
        )
        .map_err(value_error)?;
        self.0
            .card_with_options(
                reference_date,
                sex.map(SexInput::into_sex).transpose()?,
                options,
            )
            .map(Card)
            .map_err(value_error)
    }
}
