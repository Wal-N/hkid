mod card;
mod generator;
mod name;
mod number;
mod symbols;

use pyo3::exceptions::{PyUnicodeEncodeError, PyValueError};
use pyo3::prelude::*;
use pyo3::types::{PyDate, PyString};

fn today(py: Python<'_>) -> PyResult<hkid_core::NaiveDate> {
    py.get_type::<PyDate>().call_method0("today")?.extract()
}

fn value_error(error: impl std::fmt::Display) -> PyErr {
    PyValueError::new_err(error.to_string())
}

// A Python string may contain a lone surrogate, which cannot occur in Rust text.
fn validation_text<'a>(value: Option<&'a Bound<'_, PyString>>) -> PyResult<Option<&'a str>> {
    let Some(value) = value else {
        return Ok(None);
    };
    match value.to_str() {
        Ok(text) => Ok(Some(text)),
        Err(error) if error.is_instance_of::<PyUnicodeEncodeError>(value.py()) => Ok(None),
        Err(error) => Err(error),
    }
}

/// Hong Kong identity card validation, formatting and test data generation.
#[pyo3::pymodule]
mod hkid_utils {
    use hkid_core::HkidNumber;
    use pyo3::prelude::*;
    use pyo3::types::PyString;

    #[pymodule_init]
    fn init(module: &Bound<'_, PyModule>) -> PyResult<()> {
        crate::number::register(module)?;
        crate::symbols::register(module)?;
        crate::card::register(module)?;
        crate::name::register(module)?;
        module.add_class::<crate::generator::Generator>()
    }

    /// Return whether the number is valid, checking any supplied digit. None returns False.
    #[pyfunction(signature = (value))]
    fn is_valid(value: Option<&Bound<'_, PyString>>) -> PyResult<bool> {
        Ok(crate::validation_text(value)?.is_some_and(HkidNumber::is_valid))
    }

    fn parse_number(value: &str) -> PyResult<HkidNumber> {
        value.parse::<HkidNumber>().map_err(crate::value_error)
    }

    /// Return the calculated check digit (0–9 or A). Invalid numbers raise ValueError.
    #[pyfunction]
    fn check_digit(value: &str) -> PyResult<char> {
        Ok(parse_number(value)?.check_digit())
    }

    /// Return the uppercase number with its check digit in parentheses. Invalid numbers raise ValueError.
    #[pyfunction]
    fn format_number(value: &str) -> PyResult<String> {
        Ok(parse_number(value)?.to_string())
    }
}
