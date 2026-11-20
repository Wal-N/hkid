//! Chinese commercial codes used alongside a printed Chinese name.

use std::{error::Error, fmt};

/// Identifies which part of a Chinese commercial code has invalid syntax.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum CommercialCodeError {
    /// The main code must contain exactly four ASCII digits.
    InvalidCode,
    /// A supplied extension must contain exactly one ASCII digit.
    InvalidExtension,
}

impl fmt::Display for CommercialCodeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::InvalidCode => "commercial code must be four ASCII digits",
            Self::InvalidExtension => "commercial code extension must be one ASCII digit",
        })
    }
}

impl Error for CommercialCodeError {}

/// A four-digit code and its optional one-digit extension.
///
/// Equality and hashing include the extension. Display prints the main code
/// alone. Use both accessors when storing or exchanging the complete value.
#[derive(Clone, Debug, Eq, Hash, PartialEq)]
pub struct ChineseCommercialCode {
    code: String,
    extension_number: Option<String>,
}

impl ChineseCommercialCode {
    /// Checks the digit syntax and preserves leading zeros in both fields.
    /// Character-to-code assignments are outside the scope of this check.
    pub fn new(code: &str, extension_number: Option<&str>) -> Result<Self, CommercialCodeError> {
        if !has_ascii_digits(code, 4) {
            return Err(CommercialCodeError::InvalidCode);
        }
        if extension_number.is_some_and(|value| !has_ascii_digits(value, 1)) {
            return Err(CommercialCodeError::InvalidExtension);
        }
        Ok(Self {
            code: code.to_owned(),
            extension_number: extension_number.map(str::to_owned),
        })
    }

    /// The four-digit main code, including any leading zeros.
    pub fn code(&self) -> &str {
        &self.code
    }

    /// The supplied extension, including `Some("0")` when explicitly present.
    pub fn extension_number(&self) -> Option<&str> {
        self.extension_number.as_deref()
    }
}

impl fmt::Display for ChineseCommercialCode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.code())
    }
}

fn has_ascii_digits(text: &str, count: usize) -> bool {
    text.len() == count && text.bytes().all(|byte| byte.is_ascii_digit())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashSet;

    #[test]
    fn extensions_distinguish_keys_with_the_same_printed_code() {
        let mut codes = HashSet::new();
        for extension in [None, Some("0"), Some("2")] {
            let code = ChineseCommercialCode::new("0247", extension).unwrap();
            assert_eq!(code.to_string(), "0247");
            assert!(codes.insert(code));
        }
        assert!(!codes.insert(ChineseCommercialCode::new("0247", Some("2")).unwrap()));
    }
}
