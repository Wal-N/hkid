//! Printed codes, categories and descriptions for current smart HKID card symbols.

use std::collections::{BTreeMap, HashSet};
use std::fmt;
use std::sync::LazyLock;

use serde::de::IntoDeserializer;
use serde::{Deserialize, Deserializer};

use crate::json_object::Object;
use crate::text::{fold_fullwidth, trim_outer};

const SYMBOL_DATA: &str = include_str!("../../data/hkid-symbols.json");
static METADATA: LazyLock<BTreeMap<HkidSymbol, Entry>> =
    LazyLock::new(|| load_symbols(SYMBOL_DATA).expect("invalid bundled hkid-symbols.json"));

/// A symbol on a current smart HKID card, ordered as it appears in canonical output.
#[derive(Clone, Copy, Debug, Deserialize, Eq, Hash, Ord, PartialEq, PartialOrd)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum HkidSymbol {
    AdultReEntryPermit,
    MinorReEntryPermit,
    RightOfAbode,
    StayLimited,
    RightToLand,
    StayNotLimited,
    BornInHongKong,
    BornInMainland,
    BornInMacao,
    BornElsewhere,
    BirthDetailsChanged,
    NameChanged,
}

impl HkidSymbol {
    /// All supported symbols in canonical card order.
    pub fn all() -> &'static [Self] {
        &[
            Self::AdultReEntryPermit,
            Self::MinorReEntryPermit,
            Self::RightOfAbode,
            Self::StayLimited,
            Self::RightToLand,
            Self::StayNotLimited,
            Self::BornInHongKong,
            Self::BornInMainland,
            Self::BornInMacao,
            Self::BornElsewhere,
            Self::BirthDetailsChanged,
            Self::NameChanged,
        ]
    }

    /// Looks up one complete printed code, accepting lowercase and fullwidth letters.
    /// Trims the approved outer whitespace and requires one intact code.
    pub fn lookup(input: &str) -> Option<Self> {
        let (symbol, tail) = Self::take_from(trim_outer(input))?;
        tail.is_empty().then_some(symbol)
    }

    // Match the longest complete printed code so that "***" is one symbol.
    pub(crate) fn take_from(input: &str) -> Option<(Self, &str)> {
        METADATA
            .values()
            .filter_map(|entry| {
                let mut remaining = input.chars();
                for expected in entry.code.chars() {
                    if fold_fullwidth(remaining.next()?).to_ascii_uppercase() != expected {
                        return None;
                    }
                }
                Some((entry.id, remaining.as_str()))
            })
            .min_by_key(|(_, tail)| tail.len())
    }

    /// The code printed on the card.
    pub fn code(self) -> &'static str {
        &METADATA[&self].code
    }

    /// The category used to validate this symbol alongside others.
    pub fn category(self) -> HkidSymbolCategory {
        METADATA[&self].category
    }

    /// English description from the shared symbol table.
    pub fn description(self) -> &'static str {
        &METADATA[&self].descriptions.en
    }

    /// Traditional Chinese description from the shared symbol table.
    pub fn traditional_chinese_description(self) -> &'static str {
        &METADATA[&self].descriptions.zh_hant
    }
}

impl fmt::Display for HkidSymbol {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter.write_str(self.code())
    }
}

/// A category of printed symbols, with its card-level combination rule.
#[derive(Clone, Copy, Debug, Deserialize, Eq, Hash, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum HkidSymbolCategory {
    ReEntryPermitEligibility,
    ResidentialStatus,
    ReportedPlaceOfBirth,
    OtherInformation,
}

impl HkidSymbolCategory {
    /// Whether several distinct symbols of this category may coexist on one card.
    pub fn allows_multiple(self) -> bool {
        self == Self::OtherInformation
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Table {
    schema_version: u32,
    symbols: Vec<Object<Entry>>,
}

#[derive(Debug, Deserialize)]
#[serde(deny_unknown_fields)]
struct Entry {
    #[serde(deserialize_with = "string_key")]
    id: HkidSymbol,
    code: String,
    #[serde(deserialize_with = "string_key")]
    category: HkidSymbolCategory,
    descriptions: Object<Descriptions>,
}

#[derive(Debug, Deserialize)]
#[serde(deny_unknown_fields)]
struct Descriptions {
    en: String,
    #[serde(rename = "zh-Hant")]
    zh_hant: String,
}

// The shared schema uses string keys. Serde also accepts externally tagged objects for enums.
fn string_key<'de, D: Deserializer<'de>, T: Deserialize<'de>>(input: D) -> Result<T, D::Error> {
    T::deserialize(String::deserialize(input)?.into_deserializer())
}

fn load_symbols(input: &str) -> Result<BTreeMap<HkidSymbol, Entry>, String> {
    let Object(table): Object<Table> =
        serde_json::from_str(input).map_err(|error| error.to_string())?;
    if table.schema_version != 1 {
        return Err(format!(
            "unsupported symbol schema version {}",
            table.schema_version
        ));
    }
    let mut by_symbol = BTreeMap::new();
    let mut codes = HashSet::new();
    for Object(entry) in table.symbols {
        let symbol = entry.id;
        if entry.code.is_empty()
            || entry.descriptions.en.is_empty()
            || entry.descriptions.zh_hant.is_empty()
        {
            return Err(format!("empty code or description for symbol {symbol:?}"));
        }
        if !codes.insert(entry.code.clone()) {
            return Err(format!("duplicate symbol code {:?}", entry.code));
        }
        if by_symbol.insert(symbol, entry).is_some() {
            return Err(format!("duplicate symbol {symbol:?}"));
        }
    }
    for symbol in HkidSymbol::all() {
        if !by_symbol.contains_key(symbol) {
            return Err(format!("missing symbol {symbol:?}"));
        }
    }
    Ok(by_symbol)
}

#[cfg(test)]
mod tests {
    use serde_json::{Value, json};

    use super::*;

    #[test]
    fn lookup_requires_one_intact_code_and_exposes_both_descriptions() {
        let symbol = HkidSymbol::lookup("\tｚ\u{3000}").unwrap();
        assert_eq!(symbol, HkidSymbol::BornInHongKong);
        assert_eq!(symbol.category(), HkidSymbolCategory::ReportedPlaceOfBirth);
        assert_eq!(symbol.to_string(), "Z");
        let data: Value = serde_json::from_str(SYMBOL_DATA).unwrap();
        let row = data["symbols"]
            .as_array()
            .unwrap()
            .iter()
            .find(|row| row["id"] == "BORN_IN_HONG_KONG")
            .unwrap();
        assert_eq!(symbol.description(), row["descriptions"]["en"]);
        assert_eq!(
            symbol.traditional_chinese_description(),
            row["descriptions"]["zh-Hant"]
        );
        for invalid in ["* * *", "＊＊＊", "AZ", "Ⓐ", "\u{200b}A", ""] {
            assert_eq!(HkidSymbol::lookup(invalid), None, "{invalid:?}");
        }
    }

    #[test]
    fn symbol_order_is_independent_of_metadata_row_order() {
        let mut data: Value = serde_json::from_str(SYMBOL_DATA).unwrap();
        data["symbols"].as_array_mut().unwrap().reverse();
        let metadata = load_symbols(&data.to_string()).unwrap();
        assert!(
            metadata
                .keys()
                .copied()
                .eq(HkidSymbol::all().iter().copied())
        );
        assert_eq!(metadata[&HkidSymbol::AdultReEntryPermit].code, "***");
    }

    #[test]
    fn table_rejects_unknown_missing_or_duplicate_symbols_and_codes() {
        let data: Value = serde_json::from_str(SYMBOL_DATA).unwrap();
        let first = &data["symbols"][0];
        for (key, value) in [
            ("id", json!("UNKNOWN")),
            ("id", first["id"].clone()),
            ("code", first["code"].clone()),
            ("code", json!("")),
            ("category", json!("UNKNOWN")),
            ("id", json!({"RIGHT_OF_ABODE": null})),
            ("category", json!({"OTHER_INFORMATION": null})),
            ("descriptions", json!(["English", "中文"])),
            ("descriptions", json!({"en": "", "zh-Hant": "中文"})),
            ("descriptions", json!({"en": "English"})),
        ] {
            let mut invalid = data.clone();
            invalid["symbols"][1][key] = value;
            assert!(load_symbols(&invalid.to_string()).is_err(), "{key}");
        }
        let mut missing = data.clone();
        missing["symbols"].as_array_mut().unwrap().pop();
        assert!(load_symbols(&missing.to_string()).is_err());
        let mut unsupported = data;
        unsupported["schemaVersion"] = json!(2);
        assert!(load_symbols(&unsupported.to_string()).is_err());
    }
}
