# HKID Utilities for Rust

`hkid-utils` validates and formats Hong Kong identity card numbers, smart-card
symbols, Chinese and English names, and Chinese Commercial Codes. It also looks
up defined prefixes and symbol descriptions.

## Numbers and prefixes

```rust
use hkid_utils::{DefinedPrefix, HkidNumber, ParseNumberError};

let number: HkidNumber = "Ａ 123456 （３）".parse().unwrap();
assert_eq!(number.to_string(), "A123456(3)");
assert_eq!(number.masked(), "****456(*)");
assert_eq!("A123456(4)".parse::<HkidNumber>(), Err(ParseNumberError::InvalidCheckDigit));
assert!(HkidNumber::is_valid_ascii_without_whitespace("A123456(3)"));

let prefix = DefinedPrefix::lookup("ｗｘ").unwrap();
assert_eq!(prefix.id(), "WX");

// Both first-issue prefixes were in use during the July 2026 transition.
let july = DefinedPrefix::from_first_issue_month(2026, 7);
let ids: Vec<_> = july.iter().map(|prefix| prefix.id()).collect();
assert_eq!(ids, ["F", "L"]);
```

The parser accepts a body with one or two letters and six digits. It derives
an omitted check digit and checks any supplied digit. Use
`HkidNumber::from_parts` for separate ASCII components.

## Smart-card symbols

```rust
use hkid_utils::{HkidSymbol, HkidSymbols};

let symbols: HkidSymbols = "ｚ *** Ａ".parse().unwrap();
assert_eq!(symbols.to_string(), "***AZ");
assert!(symbols.contains(HkidSymbol::RightOfAbode));
assert!(HkidSymbols::is_valid("BN"));
assert!(!HkidSymbols::is_valid("AC"));

let birth = "2004-02-29".parse().unwrap();
let registration = "2022-03-01".parse().unwrap();
assert!(symbols.validate_age(birth, registration).is_ok());
```

Parsing checks for duplicates and conflicting symbols and returns them in
card order. Use the registration date to check age eligibility. Someone born
on 29 February reaches the next age on 1 March in a non-leap year.

When editing `data/hkid-symbols.json`, keep `***` for the adult permit symbol
and `*` for the minor permit symbol. Other printed codes use uppercase ASCII
letters. Each must be unique, and no letter code may start with another code
in the table. For example, defining both `A` and `AZ` would make adjacent
symbols ambiguous. Use one letter per code for compatibility with the Java parser.

## Names and Chinese Commercial Codes

```rust
use hkid_utils::{ChineseCommercialCode, ChineseName, EnglishName};

let codes = vec![
    ChineseCommercialCode::new("7115", None).unwrap(),
    ChineseCommercialCode::new("2479", Some("0")).unwrap(),
];
let chinese = ChineseName::with_commercial_codes(Some("陳"), Some("旻"), codes).unwrap();
assert_eq!(chinese.to_string(), "陳旻");
let second = &chinese.commercial_codes()[1];
assert_eq!(second.to_string(), "2479");
assert_eq!(second.extension_number(), Some("0"));

let english = EnglishName::new(Some("Chan"), Some("Tai Man")).unwrap();
assert_eq!(english.to_string(), "Chan, Tai Man");
assert_eq!(EnglishName::new(None, Some("Tai Man")).unwrap().to_string(), "Tai Man");
assert!(!EnglishName::is_valid(None, Some("Tai Man")));
```

A Chinese name can contain up to six Han letters. Each letter counts once,
including supplementary-plane characters. Use `ChineseName::new` for a name
without codes. If you supply codes, give one per character in printed order.
Each code has four ASCII digits and an optional single-digit extension. The
extension is part of the value. Only the four main digits appear on display.

English names retain their spelling and case. Within each part, ASCII letter
groups may be joined by a single space, period, apostrophe or hyphen. The
formatted name can contain up to 40 characters, including the comma and space.
Either name part can be absent. Complete English-name validation requires a surname.

## Build and test

Run the following commands from the repository root with Rust 1.100.0
(2024 edition). The package includes the shared number, symbol and name fixtures
and embeds the prefix and symbol tables.

```sh
cargo fmt --check
cargo clippy --locked --all-targets -- -D warnings
cargo test --locked
cargo package --locked
```
