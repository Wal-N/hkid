use hkid_core::{HkidSymbol, HkidSymbolCategory, HkidSymbols, NaiveDate};
use pyo3::prelude::*;
use pyo3::types::{PyIterator, PyList, PyString};

use crate::{validation_text, value_error};

/// A card symbol category and its combination rule.
#[pyclass(name = "HkidSymbolCategory", module = "hkid_utils", frozen, eq, hash)]
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct Category(HkidSymbolCategory);

#[pymethods]
impl Category {
    #[classattr]
    const RE_ENTRY_PERMIT_ELIGIBILITY: Self = Self(HkidSymbolCategory::ReEntryPermitEligibility);
    #[classattr]
    const RESIDENTIAL_STATUS: Self = Self(HkidSymbolCategory::ResidentialStatus);
    #[classattr]
    const REPORTED_PLACE_OF_BIRTH: Self = Self(HkidSymbolCategory::ReportedPlaceOfBirth);
    #[classattr]
    const OTHER_INFORMATION: Self = Self(HkidSymbolCategory::OtherInformation);

    #[getter]
    fn allows_multiple(&self) -> bool {
        self.0.allows_multiple()
    }

    fn __str__(&self) -> &'static str {
        match self.0 {
            HkidSymbolCategory::ReEntryPermitEligibility => "RE_ENTRY_PERMIT_ELIGIBILITY",
            HkidSymbolCategory::ResidentialStatus => "RESIDENTIAL_STATUS",
            HkidSymbolCategory::ReportedPlaceOfBirth => "REPORTED_PLACE_OF_BIRTH",
            HkidSymbolCategory::OtherInformation => "OTHER_INFORMATION",
        }
    }
}

/// A single printed symbol with English and Traditional Chinese descriptions.
#[pyclass(name = "HkidSymbol", module = "hkid_utils", frozen, eq, hash)]
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct Symbol(pub(crate) HkidSymbol);

#[pymethods]
impl Symbol {
    #[classattr]
    const ADULT_RE_ENTRY_PERMIT: Self = Self(HkidSymbol::AdultReEntryPermit);
    #[classattr]
    const MINOR_RE_ENTRY_PERMIT: Self = Self(HkidSymbol::MinorReEntryPermit);
    #[classattr]
    const RIGHT_OF_ABODE: Self = Self(HkidSymbol::RightOfAbode);
    #[classattr]
    const STAY_LIMITED: Self = Self(HkidSymbol::StayLimited);
    #[classattr]
    const RIGHT_TO_LAND: Self = Self(HkidSymbol::RightToLand);
    #[classattr]
    const STAY_NOT_LIMITED: Self = Self(HkidSymbol::StayNotLimited);
    #[classattr]
    const BORN_IN_HONG_KONG: Self = Self(HkidSymbol::BornInHongKong);
    #[classattr]
    const BORN_IN_MAINLAND: Self = Self(HkidSymbol::BornInMainland);
    #[classattr]
    const BORN_IN_MACAO: Self = Self(HkidSymbol::BornInMacao);
    #[classattr]
    const BORN_ELSEWHERE: Self = Self(HkidSymbol::BornElsewhere);
    #[classattr]
    const BIRTH_DETAILS_CHANGED: Self = Self(HkidSymbol::BirthDetailsChanged);
    #[classattr]
    const NAME_CHANGED: Self = Self(HkidSymbol::NameChanged);

    #[new]
    fn new(code: &str) -> PyResult<Self> {
        HkidSymbol::lookup(code)
            .map(Self)
            .ok_or_else(|| value_error("expected a single HKID symbol code"))
    }

    #[staticmethod]
    fn all() -> Vec<Self> {
        HkidSymbol::all().iter().copied().map(Self).collect()
    }

    /// Return the symbol for a printed code, or None if unrecognized.
    #[staticmethod]
    #[pyo3(signature = (code))]
    fn lookup(code: Option<&Bound<'_, PyString>>) -> PyResult<Option<Self>> {
        Ok(validation_text(code)?
            .and_then(HkidSymbol::lookup)
            .map(Self))
    }

    #[getter]
    fn code(&self) -> &'static str {
        self.0.code()
    }

    #[getter]
    fn category(&self) -> Category {
        Category(self.0.category())
    }

    #[getter]
    fn description(&self) -> &'static str {
        self.0.description()
    }

    #[getter]
    fn traditional_chinese_description(&self) -> &'static str {
        self.0.traditional_chinese_description()
    }

    fn __str__(&self) -> &'static str {
        self.0.code()
    }
}

#[derive(FromPyObject)]
pub(crate) enum SymbolInput<'py> {
    #[pyo3(transparent)]
    Symbol(PyRef<'py, Symbol>),
    #[pyo3(transparent)]
    Text(String),
}

impl SymbolInput<'_> {
    pub(crate) fn into_symbol(self) -> PyResult<HkidSymbol> {
        match self {
            Self::Symbol(symbol) => Ok(symbol.0),
            Self::Text(code) => Symbol::new(&code).map(|symbol| symbol.0),
        }
    }
}

/// Distinct, compatible symbols in the order printed on the card.
#[pyclass(name = "HkidSymbols", module = "hkid_utils", frozen, eq, hash)]
#[derive(PartialEq, Eq, Hash)]
pub(crate) struct Symbols(pub(crate) HkidSymbols);

#[pymethods]
impl Symbols {
    #[new]
    #[pyo3(signature = (codes = ""))]
    fn new(codes: &str) -> PyResult<Self> {
        codes.parse().map(Self).map_err(value_error)
    }

    /// Construct a collection from an iterable of individual symbols or codes.
    #[staticmethod]
    fn from_symbols(symbols: &Bound<'_, PyAny>) -> PyResult<Self> {
        let values = symbols
            .try_iter()?
            .map(|item| item?.extract::<SymbolInput<'_>>()?.into_symbol())
            .collect::<PyResult<Vec<_>>>()?;
        HkidSymbols::from_symbols(values)
            .map(Self)
            .map_err(value_error)
    }

    #[staticmethod]
    #[pyo3(signature = (value))]
    fn is_valid(value: Option<&Bound<'_, PyString>>) -> PyResult<bool> {
        Ok(validation_text(value)?.is_some_and(HkidSymbols::is_valid))
    }

    fn by_category(&self, category: &Category) -> Vec<Symbol> {
        self.0.by_category(category.0).map(Symbol).collect()
    }

    /// Raise ValueError if the holder's age is incompatible with the symbols.
    fn validate_age(&self, date_of_birth: NaiveDate, reference_date: NaiveDate) -> PyResult<()> {
        self.0
            .validate_age(date_of_birth, reference_date)
            .map_err(value_error)
    }

    fn __str__(&self) -> String {
        self.0.to_string()
    }

    fn __len__(&self) -> usize {
        self.0.as_slice().len()
    }

    fn __contains__(&self, symbol: SymbolInput<'_>) -> PyResult<bool> {
        Ok(self.0.contains(symbol.into_symbol()?))
    }

    fn __iter__<'py>(&self, py: Python<'py>) -> PyResult<Bound<'py, PyIterator>> {
        PyList::new(py, self.0.as_slice().iter().copied().map(Symbol))?.try_iter()
    }
}

pub(crate) fn register(module: &Bound<'_, PyModule>) -> PyResult<()> {
    module.add_class::<Category>()?;
    module.add_class::<Symbol>()?;
    module.add_class::<Symbols>()
}
