#![doc = include_str!("../README.md")]

pub mod commercial_code;
mod json_object;
pub mod name;
pub mod number;
pub mod prefix;
pub mod symbol;
pub mod symbols;
mod text;

pub use commercial_code::{ChineseCommercialCode, CommercialCodeError};
pub use name::{ChineseName, EnglishName, NameError};
pub use number::{HkidNumber, ParseNumberError};
pub use prefix::DefinedPrefix;
pub use symbol::{HkidSymbol, HkidSymbolCategory};
pub use symbols::{HkidSymbols, ParseSymbolsError, SymbolAgeError};
