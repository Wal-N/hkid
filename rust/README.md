# HKID Utilities for Rust

`hkid-utils` validates and formats Hong Kong identity card numbers and
smart-card symbols. It also looks up defined prefixes and symbol descriptions.

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

## Build and test

Run the following commands from the repository root with Rust 1.100.0
(2024 edition). The package includes the shared number and symbol fixtures
and embeds the prefix and symbol tables.

```sh
cargo fmt --check
cargo clippy --locked --all-targets -- -D warnings
cargo test --locked
cargo package --locked
```
