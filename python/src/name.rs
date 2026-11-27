use pyo3::{prelude::*, types::PyString};

use crate::card::{Sex, SexInput};
use crate::{validation_text, value_error};

/// A four-digit Chinese commercial code, with an optional one-digit extension.
#[pyclass(
    name = "ChineseCommercialCode",
    module = "hkid_utils",
    frozen,
    eq,
    hash,
    from_py_object
)]
#[derive(Clone, PartialEq, Hash)]
pub(crate) struct CommercialCode(pub(crate) hkid_core::ChineseCommercialCode);

#[pymethods]
impl CommercialCode {
    #[new]
    #[pyo3(signature = (code, extension_number=None))]
    fn new(code: &str, extension_number: Option<&str>) -> PyResult<Self> {
        hkid_core::ChineseCommercialCode::new(code, extension_number)
            .map(Self)
            .map_err(value_error)
    }

    /// Check the syntax of the main code and any supplied extension.
    #[staticmethod]
    #[pyo3(signature = (code, extension_number=None))]
    fn is_valid(
        code: Option<&Bound<'_, PyString>>,
        extension_number: Option<&Bound<'_, PyString>>,
    ) -> PyResult<bool> {
        let Some(code) = validation_text(code)? else {
            return Ok(false);
        };
        let extension = validation_text(extension_number)?;
        Ok((extension_number.is_none() || extension.is_some())
            && hkid_core::ChineseCommercialCode::new(code, extension).is_ok())
    }

    #[getter]
    fn code(&self) -> &str {
        self.0.code()
    }

    #[getter]
    fn extension_number(&self) -> Option<&str> {
        self.0.extension_number()
    }

    fn __str__(&self) -> &str {
        self.0.code()
    }
}

// A missing name part represents the empty string. An unpaired surrogate is invalid.
fn name_part<'a>(value: Option<&'a Bound<'_, PyString>>) -> PyResult<Option<&'a str>> {
    if value.is_none() {
        Ok(Some(""))
    } else {
        validation_text(value)
    }
}

/// A printed Chinese name with optional commercial codes in character order.
#[pyclass(module = "hkid_utils", frozen, eq, hash, from_py_object)]
#[derive(Clone, PartialEq, Hash)]
pub(crate) struct ChineseName(pub(crate) hkid_core::ChineseName);

#[pymethods]
impl ChineseName {
    #[classattr]
    const MAX_LENGTH: usize = hkid_core::ChineseName::MAX_LENGTH;

    #[new]
    #[pyo3(signature = (surname=None, personal_name=None, commercial_codes=None))]
    fn new(
        surname: Option<&str>,
        personal_name: Option<&str>,
        commercial_codes: Option<Vec<CommercialCode>>,
    ) -> PyResult<Self> {
        let codes = commercial_codes
            .unwrap_or_default()
            .into_iter()
            .map(|code| code.0)
            .collect();
        hkid_core::ChineseName::with_commercial_codes(surname, personal_name, codes)
            .map(Self)
            .map_err(value_error)
    }

    /// Check for a nonempty sequence of Han letters without a length limit.
    #[staticmethod]
    #[pyo3(signature = (value))]
    fn is_valid_part(value: Option<&Bound<'_, PyString>>) -> PyResult<bool> {
        Ok(validation_text(value)?.is_some_and(hkid_core::ChineseName::is_valid_part))
    }

    /// Check both name parts and the number of commercial codes, when supplied.
    #[staticmethod]
    #[pyo3(signature = (surname=None, personal_name=None, commercial_codes=None))]
    fn is_valid(
        surname: Option<&Bound<'_, PyString>>,
        personal_name: Option<&Bound<'_, PyString>>,
        commercial_codes: Option<Vec<Option<CommercialCode>>>,
    ) -> PyResult<bool> {
        let Some(surname) = name_part(surname)? else {
            return Ok(false);
        };
        let Some(personal_name) = name_part(personal_name)? else {
            return Ok(false);
        };
        let codes: Option<Vec<_>> = commercial_codes
            .unwrap_or_default()
            .into_iter()
            .map(|code| code.map(|code| code.0))
            .collect();
        Ok(codes.is_some_and(|codes| {
            hkid_core::ChineseName::with_commercial_codes(Some(surname), Some(personal_name), codes)
                .is_ok()
        }))
    }

    /// Check that a collection fits the name field and contains no missing code.
    #[staticmethod]
    #[pyo3(signature = (codes))]
    fn are_valid_commercial_codes(codes: Option<Vec<Option<CommercialCode>>>) -> bool {
        codes.is_none_or(|codes| {
            codes.len() <= Self::MAX_LENGTH && codes.iter().all(Option::is_some)
        })
    }

    #[getter]
    fn surname(&self) -> &str {
        self.0.surname()
    }

    #[getter]
    fn personal_name(&self) -> &str {
        self.0.personal_name()
    }

    #[getter]
    fn commercial_codes(&self) -> Vec<CommercialCode> {
        self.0
            .commercial_codes()
            .iter()
            .cloned()
            .map(CommercialCode)
            .collect()
    }

    fn __str__(&self) -> String {
        self.0.to_string()
    }
}

/// An English surname and personal name as printed on the card.
#[pyclass(module = "hkid_utils", frozen, eq, hash, from_py_object)]
#[derive(Clone, PartialEq, Hash)]
pub(crate) struct EnglishName(pub(crate) hkid_core::EnglishName);

#[pymethods]
impl EnglishName {
    #[classattr]
    const MAX_LENGTH: usize = hkid_core::EnglishName::MAX_LENGTH;

    #[new]
    #[pyo3(signature = (surname=None, personal_name=None))]
    fn new(surname: Option<&str>, personal_name: Option<&str>) -> PyResult<Self> {
        hkid_core::EnglishName::new(surname, personal_name)
            .map(Self)
            .map_err(value_error)
    }

    /// Check the syntax of a nonempty name part without a length limit.
    #[staticmethod]
    #[pyo3(signature = (value))]
    fn is_valid_part(value: Option<&Bound<'_, PyString>>) -> PyResult<bool> {
        Ok(validation_text(value)?.is_some_and(hkid_core::EnglishName::is_valid_part))
    }

    /// Check a complete name, including its surname and printed length.
    #[staticmethod]
    #[pyo3(signature = (surname=None, personal_name=None))]
    fn is_valid(
        surname: Option<&Bound<'_, PyString>>,
        personal_name: Option<&Bound<'_, PyString>>,
    ) -> PyResult<bool> {
        let parts = name_part(surname)?.zip(name_part(personal_name)?);
        Ok(parts.is_some_and(|(surname, personal)| {
            hkid_core::EnglishName::is_valid(Some(surname), Some(personal))
        }))
    }

    #[getter]
    fn surname(&self) -> &str {
        self.0.surname()
    }

    #[getter]
    fn personal_name(&self) -> &str {
        self.0.personal_name()
    }

    fn __str__(&self) -> String {
        self.0.to_string()
    }
}

/// A character, its romanisation and selection weight in the bundled name seed.
#[pyclass(name = "ChineseNameEntry", module = "hkid_utils", frozen, eq)]
#[derive(PartialEq)]
pub(crate) struct NameEntry(pub(crate) &'static hkid_core::ChineseNameEntry);

#[pymethods]
impl NameEntry {
    /// Return every bundled entry in seed-file order.
    #[staticmethod]
    fn all() -> PyResult<Vec<Self>> {
        Ok(hkid_core::ChineseNameEntry::all()
            .map_err(value_error)?
            .iter()
            .map(Self)
            .collect())
    }

    #[getter]
    fn character(&self) -> char {
        self.0.character()
    }

    #[getter]
    fn commercial_code(&self) -> CommercialCode {
        CommercialCode(self.0.commercial_code().clone())
    }

    #[getter]
    fn romanisation(&self) -> &str {
        self.0.romanisation()
    }

    #[getter]
    fn is_common_surname(&self) -> bool {
        self.0.is_common_surname()
    }

    #[getter]
    fn supported_sexes(&self) -> Vec<Sex> {
        self.0.supported_sexes().iter().copied().map(Sex).collect()
    }

    fn supports_sex(&self, sex: SexInput<'_>) -> PyResult<bool> {
        Ok(self.0.supports_sex(sex.into_sex()?))
    }

    #[getter]
    fn weight(&self) -> u64 {
        self.0.weight()
    }
}

/// A generated Chinese name with its matching romanisation.
#[pyclass(module = "hkid_utils", frozen, eq, from_py_object)]
#[derive(Clone, PartialEq)]
pub(crate) struct GeneratedName(pub(crate) hkid_core::GeneratedName);

#[pymethods]
impl GeneratedName {
    #[new]
    fn new(chinese: ChineseName, english: EnglishName) -> Self {
        Self(hkid_core::GeneratedName {
            chinese: chinese.0,
            english: english.0,
        })
    }

    #[getter]
    fn chinese(&self) -> ChineseName {
        ChineseName(self.0.chinese.clone())
    }

    #[getter]
    fn english(&self) -> EnglishName {
        EnglishName(self.0.english.clone())
    }
}

pub(crate) fn register(module: &Bound<'_, PyModule>) -> PyResult<()> {
    module.add_class::<CommercialCode>()?;
    module.add_class::<ChineseName>()?;
    module.add_class::<EnglishName>()?;
    module.add_class::<NameEntry>()?;
    module.add_class::<GeneratedName>()
}
