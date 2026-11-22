#![doc = include_str!("../README.md")]

pub mod card;
pub mod commercial_code;
pub mod generator;
pub mod holder;
mod json_object;
pub mod name;
pub mod number;
pub mod prefix;
pub mod symbol;
pub mod symbols;
mod text;

pub use card::{CardError, HkidCard, HkidCardFields, Sex, YearMonth};
pub use chrono::NaiveDate;
pub use commercial_code::{ChineseCommercialCode, CommercialCodeError};
pub use generator::{GeneratedName, GenerationError, HkidGenerator};
pub use holder::HkidSuspicion;
pub use name::{ChineseName, EnglishName, NameError};
pub use number::{HkidNumber, ParseNumberError, ParseNumberWithCheckDigitError};
pub use prefix::DefinedPrefix;
pub use symbol::{HkidSymbol, HkidSymbolCategory};
pub use symbols::{HkidSymbols, ParseSymbolsError, SymbolAgeError};
