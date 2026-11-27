use std::hash::{Hash, Hasher};

use hkid_core::{DefinedPrefix, HkidNumber, NaiveDate};
use pyo3::prelude::*;
use pyo3::types::PyString;

use crate::{validation_text, value_error};

/// A parsed HKID number with its normalized prefix, six numerals and check digit.
#[pyclass(
    name = "HkidNumber",
    module = "hkid_utils",
    frozen,
    eq,
    hash,
    from_py_object
)]
#[derive(Clone, PartialEq, Eq, Hash)]
pub(crate) struct Number(pub(crate) HkidNumber);

#[pymethods]
impl Number {
    /// Parse a number and verify its check digit when supplied.
    #[new]
    fn new(value: &str) -> PyResult<Self> {
        value.parse::<HkidNumber>().map(Self).map_err(value_error)
    }

    /// Construct a number from separate ASCII components.
    #[staticmethod]
    #[pyo3(signature = (prefix, numerals, check_digit=None))]
    fn from_parts(prefix: &str, numerals: &str, check_digit: Option<&str>) -> PyResult<Self> {
        HkidNumber::from_parts(prefix, numerals, check_digit)
            .map(Self)
            .map_err(value_error)
    }

    /// Parse a complete number, requiring a matching check digit.
    #[staticmethod]
    fn parse_with_check_digit(value: &str) -> PyResult<Self> {
        HkidNumber::parse_with_check_digit(value)
            .map(Self)
            .map_err(value_error)
    }

    #[getter]
    fn prefix(&self) -> &str {
        self.0.prefix()
    }

    #[getter]
    fn numerals(&self) -> &str {
        self.0.numerals()
    }

    #[getter]
    fn check_digit(&self) -> char {
        self.0.check_digit()
    }

    #[getter]
    fn without_check_digit(&self) -> &str {
        self.0.without_check_digit()
    }

    #[getter]
    fn without_parentheses(&self) -> String {
        self.0.without_parentheses()
    }

    #[getter]
    fn masked(&self) -> String {
        self.0.masked()
    }

    #[getter]
    fn defined_prefix(&self) -> Option<Prefix> {
        self.0.defined_prefix().map(Prefix)
    }

    #[getter]
    fn prefix_description(&self) -> String {
        self.0.prefix_description().into_owned()
    }

    #[getter]
    fn prefix_traditional_chinese_description(&self) -> String {
        self.0.prefix_traditional_chinese_description().into_owned()
    }

    fn __str__(&self) -> String {
        self.0.to_string()
    }
}

/// A defined HKID prefix and its bundled descriptions and registration periods.
#[pyclass(
    name = "DefinedPrefix",
    module = "hkid_utils",
    frozen,
    eq,
    hash,
    from_py_object
)]
#[derive(Clone, Copy, PartialEq, Eq)]
pub(crate) struct Prefix(pub(crate) &'static DefinedPrefix);

impl Hash for Prefix {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.0.id().hash(state);
    }
}

#[pymethods]
impl Prefix {
    /// All prefixes in the order used by the shared reference table.
    #[staticmethod]
    fn all() -> Vec<Self> {
        DefinedPrefix::all().iter().map(Self).collect()
    }

    /// Find a defined prefix by its letters. Unknown prefixes return None.
    #[staticmethod]
    #[pyo3(signature = (value))]
    fn lookup(value: Option<&Bound<'_, PyString>>) -> PyResult<Option<Self>> {
        Ok(validation_text(value)?
            .and_then(DefinedPrefix::lookup)
            .map(Self))
    }

    #[getter]
    fn id(&self) -> &str {
        self.0.id()
    }

    #[getter]
    fn description(&self) -> &str {
        self.0.description()
    }

    #[getter]
    fn traditional_chinese_description(&self) -> &str {
        self.0.traditional_chinese_description()
    }

    /// Inclusive start and optional exclusive end for Hong Kong birth registration.
    #[getter]
    fn birth_registration_period(&self) -> Option<(NaiveDate, Option<NaiveDate>)> {
        self.0.birth_registration_period()
    }

    /// Inclusive start and optional exclusive end for first issue.
    #[getter]
    fn first_issue_period(&self) -> Option<(NaiveDate, Option<NaiveDate>)> {
        self.0.first_issue_period()
    }

    fn supports_birth_registration_date(&self, date: NaiveDate) -> bool {
        self.0.supports_birth_registration_date(date)
    }

    #[staticmethod]
    fn from_birth_registration_date(date: NaiveDate) -> Option<Self> {
        DefinedPrefix::from_birth_registration_date(date).map(Self)
    }

    /// Whether a day in the month is eligible, optionally limited to days on or before latest.
    #[pyo3(signature = (year, month, latest=None))]
    fn supports_first_issue_month(&self, year: i32, month: u32, latest: Option<NaiveDate>) -> bool {
        self.0.supports_first_issue_month(year, month, latest)
    }

    /// All matching prefixes in table order within the optional inclusive date bounds.
    #[staticmethod]
    #[pyo3(signature = (year, month, latest=None, *, earliest=None))]
    fn from_first_issue_month(
        year: i32,
        month: u32,
        latest: Option<NaiveDate>,
        earliest: Option<NaiveDate>,
    ) -> Vec<Self> {
        DefinedPrefix::from_first_issue_month_between(year, month, earliest, latest)
            .into_iter()
            .map(Self)
            .collect()
    }

    fn __str__(&self) -> &str {
        self.0.id()
    }
}

/// Validate an ASCII number, rejecting all whitespace. The check digit is optional.
#[pyfunction(signature = (value))]
fn is_valid_ascii_without_whitespace(value: Option<&Bound<'_, PyString>>) -> PyResult<bool> {
    Ok(validation_text(value)?.is_some_and(HkidNumber::is_valid_ascii_without_whitespace))
}

/// Validate a separate HKID body and a required, unparenthesized check digit.
#[pyfunction(signature = (body, check_digit))]
fn is_valid_check_digit(
    body: Option<&Bound<'_, PyString>>,
    check_digit: Option<&Bound<'_, PyString>>,
) -> PyResult<bool> {
    let parts = validation_text(body)?.zip(validation_text(check_digit)?);
    Ok(parts.is_some_and(|(body, digit)| HkidNumber::is_valid_check_digit(body, digit)))
}

pub(crate) fn register(module: &Bound<'_, PyModule>) -> PyResult<()> {
    module.add_class::<Number>()?;
    module.add_class::<Prefix>()?;
    module.add_function(wrap_pyfunction!(is_valid_ascii_without_whitespace, module)?)?;
    module.add_function(wrap_pyfunction!(is_valid_check_digit, module)?)?;
    Ok(())
}
