# HKID Utilities for Rust

`hkid-utils` validates and formats Hong Kong identity card numbers. It also
looks up defined prefixes using the project's shared reference data.

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

Run the following commands from the repository root with Rust 1.100.0
(2024 edition). The crate embeds `data/defined-prefixes.json`, and its
integration test reads `conformance/hkid-number.json`.

```sh
cargo fmt --check
cargo clippy --locked --all-targets -- -D warnings
cargo test --locked
cargo package --locked
```
