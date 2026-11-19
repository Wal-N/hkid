#![doc = include_str!("../README.md")]

mod json_object;
pub mod number;
pub mod prefix;

pub use number::{HkidNumber, ParseNumberError};
pub use prefix::DefinedPrefix;
