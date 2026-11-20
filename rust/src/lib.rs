#![doc = include_str!("../README.md")]

mod json_object;
pub mod number;
pub mod prefix;
pub mod symbol;
pub mod symbols;
mod text;

pub use number::{HkidNumber, ParseNumberError};
pub use prefix::DefinedPrefix;
pub use symbol::{HkidSymbol, HkidSymbolCategory};
pub use symbols::{HkidSymbols, ParseSymbolsError, SymbolAgeError};
