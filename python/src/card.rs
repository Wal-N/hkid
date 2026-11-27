use hkid_core::{HkidCard, HkidCardFields, NaiveDate, Sex as CoreSex};
use pyo3::prelude::*;
use pyo3::types::PyDict;

use crate::name::{ChineseName, EnglishName};
use crate::number::Number;
use crate::symbols::Symbols;
use crate::value_error;

/// A sex marker printed on the card. Construct with an English marker M or F.
#[pyclass(module = "hkid_utils", frozen, eq, hash)]
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct Sex(pub(crate) CoreSex);

#[pymethods]
impl Sex {
    #[classattr]
    const MALE: Self = Self(CoreSex::Male);
    #[classattr]
    const FEMALE: Self = Self(CoreSex::Female);

    #[new]
    fn new(marker: &str) -> PyResult<Self> {
        marker.parse().map(Self).map_err(value_error)
    }

    #[getter]
    fn english_marker(&self) -> &'static str {
        self.0.english_marker()
    }

    #[getter]
    fn chinese_marker(&self) -> &'static str {
        self.0.chinese_marker()
    }

    fn __str__(&self) -> String {
        self.0.to_string()
    }
}

#[derive(FromPyObject)]
pub(crate) enum SexInput<'py> {
    #[pyo3(transparent)]
    Sex(PyRef<'py, Sex>),
    #[pyo3(transparent)]
    Text(String),
}

impl SexInput<'_> {
    pub(crate) fn into_sex(self) -> PyResult<CoreSex> {
        match self {
            Self::Sex(sex) => Ok(sex.0),
            Self::Text(marker) => marker.parse().map_err(value_error),
        }
    }
}

#[derive(FromPyObject)]
enum NumberInput<'py> {
    #[pyo3(transparent)]
    Number(PyRef<'py, Number>),
    #[pyo3(transparent)]
    Text(String),
}

#[derive(FromPyObject)]
enum SymbolsInput<'py> {
    #[pyo3(transparent)]
    Symbols(PyRef<'py, Symbols>),
    #[pyo3(transparent)]
    Text(String),
}

/// An immutable smart card. Each supplied field is checked at construction.
///
/// Use datetime.date for birth and registration dates and an ISO year-month
/// string for first_registration_year_month. The number, sex and symbols
/// accept either their HKID objects or printed input strings.
#[pyclass(name = "HkidCard", module = "hkid_utils", frozen, eq, hash)]
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct Card(pub(crate) HkidCard);

#[pymethods]
impl Card {
    #[classattr]
    const SMART_CARD_START_DATE: NaiveDate = HkidCard::SMART_CARD_START_DATE;

    #[new]
    #[pyo3(signature = (*, hkid_number=None, chinese_name=None, english_name=None, sex=None,
        date_of_birth=None, symbols=None, first_registration_year_month=None, date_of_registration=None))]
    #[allow(clippy::too_many_arguments)]
    fn new(
        hkid_number: Option<NumberInput<'_>>,
        chinese_name: Option<&ChineseName>,
        english_name: Option<&EnglishName>,
        sex: Option<SexInput<'_>>,
        date_of_birth: Option<NaiveDate>,
        symbols: Option<SymbolsInput<'_>>,
        first_registration_year_month: Option<&str>,
        date_of_registration: Option<NaiveDate>,
    ) -> PyResult<Self> {
        let fields = HkidCardFields {
            hkid_number: match hkid_number {
                Some(NumberInput::Number(number)) => Some(number.0.clone()),
                Some(NumberInput::Text(text)) => Some(text.parse().map_err(value_error)?),
                None => None,
            },
            chinese_name: chinese_name.map(|name| name.0.clone()).unwrap_or_default(),
            english_name: english_name.map(|name| name.0.clone()).unwrap_or_default(),
            sex: sex.map(SexInput::into_sex).transpose()?,
            date_of_birth,
            symbols: match symbols {
                Some(SymbolsInput::Symbols(symbols)) => symbols.0.clone(),
                Some(SymbolsInput::Text(text)) => text.parse().map_err(value_error)?,
                None => Default::default(),
            },
            first_registration_year_month: first_registration_year_month
                .map(str::parse)
                .transpose()
                .map_err(value_error)?,
            date_of_registration,
        };
        HkidCard::new(fields).map(Self).map_err(value_error)
    }

    #[getter]
    fn hkid_number(&self) -> Option<Number> {
        self.0.fields().hkid_number.clone().map(Number)
    }

    #[getter]
    fn chinese_name(&self) -> ChineseName {
        ChineseName(self.0.fields().chinese_name.clone())
    }

    #[getter]
    fn english_name(&self) -> EnglishName {
        EnglishName(self.0.fields().english_name.clone())
    }

    #[getter]
    fn sex(&self) -> Option<Sex> {
        self.0.fields().sex.map(Sex)
    }

    #[getter]
    fn date_of_birth(&self) -> Option<NaiveDate> {
        self.0.fields().date_of_birth
    }

    #[getter]
    fn symbols(&self) -> Symbols {
        Symbols(self.0.fields().symbols.clone())
    }

    #[getter]
    fn first_registration_year_month(&self) -> Option<String> {
        self.0
            .fields()
            .first_registration_year_month
            .map(|month| month.to_string())
    }

    #[getter]
    fn date_of_registration(&self) -> Option<NaiveDate> {
        self.0.fields().date_of_registration
    }

    /// Build a new card with the supplied keyword fields replaced and revalidated.
    #[pyo3(signature = (**changes))]
    fn replace<'py>(
        slf: &Bound<'py, Self>,
        changes: Option<&Bound<'py, PyDict>>,
    ) -> PyResult<Bound<'py, Self>> {
        let fields = PyDict::new(slf.py());
        for key in [
            "hkid_number",
            "chinese_name",
            "english_name",
            "sex",
            "date_of_birth",
            "symbols",
            "first_registration_year_month",
            "date_of_registration",
        ] {
            fields.set_item(key, slf.getattr(key)?)?;
        }
        if let Some(changes) = changes {
            fields.update(changes.as_mapping())?;
        }
        Ok(slf.get_type().call((), Some(&fields))?.cast_into()?)
    }

    #[getter]
    fn printed_date_of_birth(&self) -> Option<String> {
        self.0.printed_date_of_birth()
    }

    #[getter]
    fn printed_first_registration_year_month(&self) -> Option<String> {
        self.0.printed_first_registration_year_month()
    }

    #[getter]
    fn printed_date_of_registration(&self) -> Option<String> {
        self.0.printed_date_of_registration()
    }

    /// Completed years on the reference date, or None if birth is unknown.
    fn age_on(&self, reference_date: NaiveDate) -> PyResult<Option<i32>> {
        self.0.age_on(reference_date).map_err(value_error)
    }

    /// Raise ValueError if any supplied date lies after the reference date.
    fn validate_as_of(&self, reference_date: NaiveDate) -> PyResult<()> {
        self.0.validate_as_of(reference_date).map_err(value_error)
    }

    /// Stable identifiers for any advisory cross-field mismatches, in reporting order.
    fn find_suspicions(&self) -> Vec<&'static str> {
        self.0
            .find_suspicions()
            .into_iter()
            .map(|reason| reason.as_str())
            .collect()
    }

    fn is_suspicious(&self) -> bool {
        self.0.is_suspicious()
    }

    /// Describe the holder today in the specified or system locale.
    #[pyo3(signature = (locale=None))]
    fn describe_holder(&self, py: Python<'_>, locale: Option<&str>) -> PyResult<String> {
        self.describe_holder_on(crate::today(py)?, locale)
    }

    /// Describe the holder as of the reference date in the specified or system locale.
    #[pyo3(signature = (reference_date, locale=None))]
    fn describe_holder_on(
        &self,
        reference_date: NaiveDate,
        locale: Option<&str>,
    ) -> PyResult<String> {
        self.0
            .describe_holder_on(locale, reference_date)
            .map_err(value_error)
    }

    fn __str__(&self) -> String {
        self.0.to_string()
    }

    fn __repr__(&self) -> String {
        self.0.to_string()
    }
}

pub(crate) fn register(module: &Bound<'_, PyModule>) -> PyResult<()> {
    module.add_class::<Sex>()?;
    module.add_class::<Card>()
}
