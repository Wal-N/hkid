/// Hong Kong identity card number validation and formatting.
#[pyo3::pymodule]
mod hkid_utils {
    use hkid_core::HkidNumber;
    use pyo3::exceptions::{PyUnicodeEncodeError, PyValueError};
    use pyo3::prelude::*;
    use pyo3::types::PyString;

    /// Return whether the number is valid, checking any supplied digit. None returns False.
    #[pyfunction(signature = (value))]
    fn is_valid(value: Option<&Bound<'_, PyString>>) -> PyResult<bool> {
        let Some(value) = value else {
            return Ok(false);
        };
        match value.to_str() {
            Ok(text) => Ok(HkidNumber::is_valid(text)),
            Err(error) if error.is_instance_of::<PyUnicodeEncodeError>(value.py()) => Ok(false),
            Err(error) => Err(error),
        }
    }

    fn parse_number(value: &str) -> PyResult<HkidNumber> {
        value
            .parse::<HkidNumber>()
            .map_err(|error| PyValueError::new_err(error.to_string()))
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
