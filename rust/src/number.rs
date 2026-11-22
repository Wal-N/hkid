//! HKID number parsing, validation and card-face formatting.

use std::{error::Error, fmt, str::FromStr};

use crate::prefix::DefinedPrefix;
use crate::text::{fold_fullwidth, is_separator, trim_outer};

/// The reason a number could not be parsed.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ParseNumberError {
    /// A component or its surrounding syntax is invalid.
    InvalidFormat,
    /// The supplied check digit is well formed but does not match the body.
    InvalidCheckDigit,
}

impl fmt::Display for ParseNumberError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::InvalidFormat => "invalid HKID number format",
            Self::InvalidCheckDigit => "incorrect HKID check digit",
        })
    }
}

impl Error for ParseNumberError {}

/// The reason a number could not be parsed with a required check digit.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ParseNumberWithCheckDigitError {
    /// A component or its surrounding syntax is invalid.
    InvalidFormat,
    /// The prefix and numerals are present, but the check digit is omitted.
    MissingCheckDigit,
    /// The supplied check digit is well formed but does not match the body.
    InvalidCheckDigit,
}

impl fmt::Display for ParseNumberWithCheckDigitError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidFormat => ParseNumberError::InvalidFormat.fmt(f),
            Self::MissingCheckDigit => f.write_str("missing HKID check digit"),
            Self::InvalidCheckDigit => ParseNumberError::InvalidCheckDigit.fmt(f),
        }
    }
}

impl Error for ParseNumberWithCheckDigitError {}

/// An uppercase prefix and six numerals, including leading zeroes.
///
/// Parse text with `str::parse`; its optional check digit can be bare or
/// enclosed in matching ASCII or fullwidth parentheses. Display always uses
/// ASCII parentheses and includes the calculated check digit.
#[derive(Clone, Debug, Eq, Hash, PartialEq)]
pub struct HkidNumber {
    body: String,
}

impl HkidNumber {
    /// Builds a number from separate ASCII components, without trimming spaces
    /// or converting fullwidth input. A missing or empty check digit is derived.
    pub fn from_parts(
        prefix: &str,
        numerals: &str,
        check_digit: Option<&str>,
    ) -> Result<Self, ParseNumberError> {
        if !(1..=2).contains(&prefix.len())
            || !prefix.bytes().all(|byte| byte.is_ascii_alphabetic())
            || numerals.len() != 6
            || !numerals.bytes().all(|byte| byte.is_ascii_digit())
        {
            return Err(ParseNumberError::InvalidFormat);
        }

        let mut body = prefix.to_ascii_uppercase();
        body.push_str(numerals);
        let number = Self { body };
        if let Some(supplied) = check_digit.filter(|text| !text.is_empty()) {
            number.verify_digit(single_char(supplied)?)?;
        }
        Ok(number)
    }

    /// Parses a complete number with a required check digit.
    ///
    /// Accepts the same text forms as `str::parse`, including bare check digits
    /// and matching ASCII or fullwidth parentheses. Returns
    /// [`ParseNumberWithCheckDigitError::MissingCheckDigit`] for a well-formed
    /// body without a check digit.
    pub fn parse_with_check_digit(input: &str) -> Result<Self, ParseNumberWithCheckDigitError> {
        use ParseNumberWithCheckDigitError as StrictError;

        match Self::parse_text(input) {
            Ok((number, true)) => Ok(number),
            Ok((_, false)) => Err(StrictError::MissingCheckDigit),
            Err(ParseNumberError::InvalidFormat) => Err(StrictError::InvalidFormat),
            Err(ParseNumberError::InvalidCheckDigit) => Err(StrictError::InvalidCheckDigit),
        }
    }

    /// Tests text using the same parsing and checksum rules as `str::parse`.
    pub fn is_valid(input: &str) -> bool {
        input.parse::<Self>().is_ok()
    }

    /// Accepts only ASCII letters, digits and parentheses in the original text.
    /// A check digit may be omitted, and any supplied digit must match.
    pub fn is_valid_ascii_without_whitespace(input: &str) -> bool {
        input
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'(' | b')'))
            && Self::is_valid(input)
    }

    /// Validates a separate body and a required, unparenthesized check digit.
    /// Both inputs accept the same outer whitespace and fullwidth alphanumerics
    /// as the text parser.
    pub fn is_valid_check_digit(body: &str, check_digit: &str) -> bool {
        let Ok((number, "")) = Self::read_body(trim_outer(body)) else {
            return false;
        };
        single_char(trim_outer(check_digit))
            .and_then(|digit| number.verify_digit(fold_fullwidth(digit)))
            .is_ok()
    }

    /// The one- or two-letter uppercase prefix.
    pub fn prefix(&self) -> &str {
        &self.body[..self.body.len() - 6]
    }

    /// The six numerals with leading zeroes preserved.
    pub fn numerals(&self) -> &str {
        &self.body[self.body.len() - 6..]
    }

    /// The predefined metadata for this prefix, when available.
    pub fn defined_prefix(&self) -> Option<&'static DefinedPrefix> {
        DefinedPrefix::lookup(self.prefix())
    }

    /// The calculated ASCII check digit, including `A` for a value of ten.
    pub fn check_digit(&self) -> char {
        // A single prefix letter has an implicit leading space of value 36.
        let mut sum = if self.prefix().len() == 1 { 9 * 36 } else { 0 };
        for (weight, byte) in (2..).zip(self.body.bytes().rev()) {
            let value = if byte.is_ascii_digit() {
                byte - b'0'
            } else {
                byte - b'A' + 10
            };
            sum += weight * u32::from(value);
        }
        match (11 - sum % 11) % 11 {
            10 => 'A',
            digit => char::from(b'0' + digit as u8),
        }
    }

    /// The prefix and six numerals, without the check digit.
    pub fn without_check_digit(&self) -> &str {
        &self.body
    }

    /// The prefix, numerals and check digit without parentheses.
    pub fn without_parentheses(&self) -> String {
        format!("{}{}", self.body, self.check_digit())
    }

    /// Masks the prefix, the first three numerals and the check digit.
    pub fn masked(&self) -> String {
        let mask = if self.prefix().len() == 1 {
            "****"
        } else {
            "*****"
        };
        format!("{mask}{}(*)", &self.numerals()[3..])
    }

    fn parse_text(input: &str) -> Result<(Self, bool), ParseNumberError> {
        let (number, suffix) = Self::read_body(trim_outer(input))?;
        let suffix = suffix.trim_start_matches(is_separator);
        if suffix.is_empty() {
            return Ok((number, false));
        }

        let supplied = suffix
            .strip_prefix('(')
            .and_then(|s| s.strip_suffix(')'))
            .or_else(|| suffix.strip_prefix('（').and_then(|s| s.strip_suffix('）')))
            .unwrap_or(suffix);
        number.verify_digit(fold_fullwidth(single_char(supplied)?))?;
        Ok((number, true))
    }

    // Consume exactly the prefix and six numerals. The caller decides which
    // suffix is valid for its operation (full number or separate body).
    fn read_body(input: &str) -> Result<(Self, &str), ParseNumberError> {
        let mut body = String::with_capacity(8);
        let mut prefix_bytes = 0;
        for character in input.chars() {
            let letter = fold_fullwidth(character);
            if !letter.is_ascii_alphabetic() {
                break;
            }
            if body.len() == 2 {
                return Err(ParseNumberError::InvalidFormat);
            }
            body.push(letter.to_ascii_uppercase());
            prefix_bytes += character.len_utf8();
        }
        if body.is_empty() {
            return Err(ParseNumberError::InvalidFormat);
        }

        let mut tail = input[prefix_bytes..]
            .trim_start_matches(is_separator)
            .chars();
        for _ in 0..6 {
            let digit = tail
                .next()
                .map(fold_fullwidth)
                .filter(char::is_ascii_digit)
                .ok_or(ParseNumberError::InvalidFormat)?;
            body.push(digit);
        }
        Ok((Self { body }, tail.as_str()))
    }

    fn verify_digit(&self, digit: char) -> Result<(), ParseNumberError> {
        let digit = digit.to_ascii_uppercase();
        if !digit.is_ascii_digit() && digit != 'A' {
            return Err(ParseNumberError::InvalidFormat);
        }
        if digit != self.check_digit() {
            return Err(ParseNumberError::InvalidCheckDigit);
        }
        Ok(())
    }
}

impl FromStr for HkidNumber {
    type Err = ParseNumberError;

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        let (number, _) = Self::parse_text(input)?;
        Ok(number)
    }
}

impl fmt::Display for HkidNumber {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}({})", self.body, self.check_digit())
    }
}

fn single_char(input: &str) -> Result<char, ParseNumberError> {
    let mut chars = input.chars();
    match (chars.next(), chars.next()) {
        (Some(character), None) => Ok(character),
        _ => Err(ParseNumberError::InvalidFormat),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn strict_ascii_validates_the_original_input() {
        for input in ["A123456", "ab1234569", "a000002(a)"] {
            assert!(
                HkidNumber::is_valid_ascii_without_whitespace(input),
                "{input}"
            );
        }
        for input in [
            "Ａ123456(3)",
            "A12３456(3)",
            "A123456(３)",
            "A123456（3）",
            " a123456",
            "A\u{00a0}123456",
            "A123456 (3)",
            "A123456(3)\r\n",
        ] {
            assert!(HkidNumber::is_valid(input), "{input}");
            assert!(
                !HkidNumber::is_valid_ascii_without_whitespace(input),
                "{input}"
            );
        }
        for input in ["", "A12345", "A123456(4)"] {
            assert!(
                !HkidNumber::is_valid_ascii_without_whitespace(input),
                "{input}"
            );
        }
    }

    #[test]
    fn separated_components_require_ascii_and_allow_an_omitted_digit() {
        let number = "A000002(A)".parse::<HkidNumber>().unwrap();
        for digit in [None, Some(""), Some("a")] {
            assert_eq!(
                HkidNumber::from_parts("a", "000002", digit),
                Ok(number.clone())
            );
        }
        for (prefix, digits, check) in [
            ("Ａ", "000002", None),
            (" A", "000002", None),
            ("A", "０００００２", None),
            ("A", "000002", Some("Ａ")),
            ("A", "000002", Some("(A)")),
        ] {
            assert_eq!(
                HkidNumber::from_parts(prefix, digits, check),
                Err(ParseNumberError::InvalidFormat)
            );
        }
        assert_eq!(
            HkidNumber::from_parts("A", "000002", Some("0")),
            Err(ParseNumberError::InvalidCheckDigit)
        );
    }
}
