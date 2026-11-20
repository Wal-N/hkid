//! Chinese and English names as printed on HKID cards.

use std::{error::Error, fmt};

use unicode_general_category::{GeneralCategory, get_general_category};
use unicode_script::{Script, UnicodeScript};

use crate::ChineseCommercialCode;

/// The reason a printed name could not be constructed.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum NameError {
    /// The surname contains unsupported characters or separators.
    InvalidSurname,
    /// The personal name contains unsupported characters or separators.
    InvalidPersonalName,
    /// The formatted name exceeds the character limit for its card field.
    TooLong { limit: usize },
    /// A non-empty list must contain one commercial code per Chinese name character.
    CommercialCodeCount { expected: usize, actual: usize },
}

impl fmt::Display for NameError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidSurname => f.write_str("invalid surname"),
            Self::InvalidPersonalName => f.write_str("invalid personal name"),
            Self::TooLong { limit } => write!(f, "printed name exceeds {limit} characters"),
            Self::CommercialCodeCount { expected, actual } => {
                write!(
                    f,
                    "expected {expected} Chinese commercial codes, received {actual}"
                )
            }
        }
    }
}

impl Error for NameError {}

/// A Chinese surname and personal name, with optional commercial codes in printed order.
#[derive(Clone, Debug, Default, Eq, Hash, PartialEq)]
pub struct ChineseName {
    surname: String,
    personal_name: String,
    commercial_codes: Vec<ChineseCommercialCode>,
}

impl ChineseName {
    /// Maximum combined number of Unicode code points on the card.
    pub const MAX_LENGTH: usize = 6;

    /// Constructs a Chinese name without commercial codes. A missing part becomes empty.
    pub fn new(surname: Option<&str>, personal_name: Option<&str>) -> Result<Self, NameError> {
        Self::with_commercial_codes(surname, personal_name, Vec::new())
    }

    /// Constructs a Chinese name, preserving any supplied codes in printed-name order.
    /// An empty list omits the codes. A non-empty list must have one entry per
    /// name character. The character-to-code mapping is not checked.
    pub fn with_commercial_codes(
        surname: Option<&str>,
        personal_name: Option<&str>,
        commercial_codes: Vec<ChineseCommercialCode>,
    ) -> Result<Self, NameError> {
        let surname = surname.unwrap_or_default();
        let personal_name = personal_name.unwrap_or_default();
        let length = Self::validate_parts(surname, personal_name)?;
        if !commercial_codes.is_empty() && commercial_codes.len() != length {
            return Err(NameError::CommercialCodeCount {
                expected: length,
                actual: commercial_codes.len(),
            });
        }
        Ok(Self {
            surname: surname.to_owned(),
            personal_name: personal_name.to_owned(),
            commercial_codes,
        })
    }

    /// Tests whether a non-empty string contains only Han-script letters, regardless of length.
    pub fn is_valid_part(value: &str) -> bool {
        !value.is_empty() && value.chars().all(is_han_letter)
    }

    /// Tests both parts against the card's character and length limits. Either part may be empty.
    pub fn is_valid(surname: Option<&str>, personal_name: Option<&str>) -> bool {
        Self::validate_parts(
            surname.unwrap_or_default(),
            personal_name.unwrap_or_default(),
        )
        .is_ok()
    }

    fn validate_parts(surname: &str, personal_name: &str) -> Result<usize, NameError> {
        let length = surname
            .chars()
            .chain(personal_name.chars())
            .take(Self::MAX_LENGTH + 1)
            .count();
        if length > Self::MAX_LENGTH {
            return Err(NameError::TooLong {
                limit: Self::MAX_LENGTH,
            });
        }
        if !surname.chars().all(is_han_letter) {
            return Err(NameError::InvalidSurname);
        }
        if !personal_name.chars().all(is_han_letter) {
            return Err(NameError::InvalidPersonalName);
        }
        Ok(length)
    }

    /// The surname, possibly empty.
    pub fn surname(&self) -> &str {
        &self.surname
    }

    /// The personal name, possibly empty.
    pub fn personal_name(&self) -> &str {
        &self.personal_name
    }

    /// Commercial codes in printed-name order, including any extensions.
    pub fn commercial_codes(&self) -> &[ChineseCommercialCode] {
        &self.commercial_codes
    }
}

impl fmt::Display for ChineseName {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}{}", self.surname, self.personal_name)
    }
}

fn is_han_letter(ch: char) -> bool {
    ch.script() == Script::Han
        && matches!(
            get_general_category(ch),
            GeneralCategory::LowercaseLetter
                | GeneralCategory::ModifierLetter
                | GeneralCategory::OtherLetter
                | GeneralCategory::TitlecaseLetter
                | GeneralCategory::UppercaseLetter
        )
}

/// An English surname and personal name. Either or both parts may be empty.
#[derive(Clone, Debug, Default, Eq, Hash, PartialEq)]
pub struct EnglishName {
    surname: String,
    personal_name: String,
}

impl EnglishName {
    /// Maximum printed length, including the comma and space between two non-empty parts.
    pub const MAX_LENGTH: usize = 40;

    /// Constructs a full or partial English name. A missing part becomes empty.
    pub fn new(surname: Option<&str>, personal_name: Option<&str>) -> Result<Self, NameError> {
        let surname = surname.unwrap_or_default();
        let personal_name = personal_name.unwrap_or_default();
        Self::validate_parts(surname, personal_name)?;
        Ok(Self {
            surname: surname.to_owned(),
            personal_name: personal_name.to_owned(),
        })
    }

    /// Tests the syntax of a non-empty part, regardless of length. ASCII letter groups
    /// may be joined by a single space, period, apostrophe, or hyphen.
    pub fn is_valid_part(value: &str) -> bool {
        value
            .split([' ', '.', '\'', '-'])
            .all(|group| !group.is_empty() && group.bytes().all(|byte| byte.is_ascii_alphabetic()))
    }

    /// Tests whether the parts form a complete English name. A surname is required.
    pub fn is_valid(surname: Option<&str>, personal_name: Option<&str>) -> bool {
        let surname = surname.unwrap_or_default();
        !surname.is_empty()
            && Self::validate_parts(surname, personal_name.unwrap_or_default()).is_ok()
    }

    fn validate_parts(surname: &str, personal_name: &str) -> Result<(), NameError> {
        let part_length = surname
            .chars()
            .chain(personal_name.chars())
            .take(Self::MAX_LENGTH + 1)
            .count();
        if part_length + english_separator(surname, personal_name).len() > Self::MAX_LENGTH {
            return Err(NameError::TooLong {
                limit: Self::MAX_LENGTH,
            });
        }
        if !surname.is_empty() && !Self::is_valid_part(surname) {
            return Err(NameError::InvalidSurname);
        }
        if !personal_name.is_empty() && !Self::is_valid_part(personal_name) {
            return Err(NameError::InvalidPersonalName);
        }
        Ok(())
    }

    /// The surname, possibly empty.
    pub fn surname(&self) -> &str {
        &self.surname
    }

    /// The personal name, possibly empty.
    pub fn personal_name(&self) -> &str {
        &self.personal_name
    }
}

fn english_separator(surname: &str, personal_name: &str) -> &'static str {
    if surname.is_empty() || personal_name.is_empty() {
        ""
    } else {
        ", "
    }
}

impl fmt::Display for EnglishName {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let separator = english_separator(&self.surname, &self.personal_name);
        write!(f, "{}{separator}{}", self.surname, self.personal_name)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn han_script_and_letter_category_are_both_required() {
        assert!(ChineseName::is_valid_part("々")); // Han-script modifier letter
        assert!(!ChineseName::is_valid_part("〇")); // Han-script letter number
        assert!(!ChineseName::is_valid_part("人\u{fe00}")); // variation selector
        assert!(!ChineseName::is_valid_part(""));
    }

    #[test]
    fn chinese_names_preserve_partial_parts() {
        let name = ChineseName::new(None, Some("大文")).unwrap();
        assert!(ChineseName::is_valid(None, Some("大文")));
        assert_eq!(name.surname(), "");
        assert_eq!(name.personal_name(), "大文");
        assert_eq!(name.to_string(), "大文");
        assert_eq!(
            ChineseName::new(None, None).unwrap(),
            ChineseName::default()
        );
    }

    #[test]
    fn english_part_syntax_accepts_long_input_and_rejects_non_ascii_or_empty_groups() {
        let long_part = "Ab-".repeat(1500) + "C";
        assert!(EnglishName::is_valid_part(&long_part));
        assert!(!EnglishName::is_valid(Some(&long_part), None));
        for part in ["", "'Chan", "Tai.-Man", "Chán", "Ａ", "O’Connor", "Chan\n"] {
            assert!(!EnglishName::is_valid_part(part), "{part:?}");
        }
    }

    #[test]
    fn non_ascii_bytes_do_not_inflate_the_formatted_length_error() {
        let surname = "陳".repeat(14);
        assert_eq!(
            EnglishName::new(Some(&surname), None),
            Err(NameError::InvalidSurname)
        );
    }
}
