//! Non-authoritative HKID prefix descriptions and registration periods.

use std::collections::HashMap;
use std::sync::LazyLock;

use chrono::{Datelike, NaiveDate};
use serde::Deserialize;

use crate::json_object::Object;
use crate::text::{fold_fullwidth, trim_outer};

const PREFIX_DATA: &str = include_str!("../../data/defined-prefixes.json");
// These schema keys also define the order of results when periods overlap.
const PREFIX_IDS: &[&str] = &[
    "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "R", "S", "T", "V", "W",
    "Y", "Z", "WX", "XA", "XB", "XC", "XD", "XE", "XG", "XH",
];
static PREFIXES: LazyLock<Vec<DefinedPrefix>> =
    LazyLock::new(|| load_prefixes(PREFIX_DATA).expect("invalid bundled defined-prefixes.json"));

/// A defined HKID prefix and its bundled historical metadata.
#[derive(Debug, PartialEq, Eq)]
pub struct DefinedPrefix(Entry);

impl DefinedPrefix {
    /// All defined prefixes in the order used by the shared lookups.
    pub fn all() -> &'static [Self] {
        &PREFIXES
    }

    /// Looks up a one- or two-letter prefix using the shared text parsing rules.
    pub fn lookup(input: &str) -> Option<&'static Self> {
        let normalized = trim_outer(input)
            .chars()
            .map(fold_fullwidth)
            .map(|c| c.to_ascii_uppercase());
        Self::all()
            .iter()
            .find(|prefix| prefix.id().chars().eq(normalized.clone()))
    }

    /// The uppercase prefix printed on the card.
    pub fn id(&self) -> &str {
        &self.0.id
    }

    /// English description from the shared reference table.
    pub fn description(&self) -> &str {
        &self.0.descriptions.en
    }

    /// Traditional Chinese description from the shared reference table.
    pub fn traditional_chinese_description(&self) -> &str {
        &self.0.descriptions.zh_hant
    }

    /// Exact Hong Kong birth-registration period as an inclusive start and optional exclusive end.
    pub fn birth_registration_period(&self) -> Option<(NaiveDate, Option<NaiveDate>)> {
        self.period(Basis::BirthRegistration)
    }

    /// Exact first-issue period as an inclusive start and optional exclusive end.
    pub fn first_issue_period(&self) -> Option<(NaiveDate, Option<NaiveDate>)> {
        self.period(Basis::FirstIssue)
    }

    fn period(&self, basis: Basis) -> Option<(NaiveDate, Option<NaiveDate>)> {
        let period = self
            .0
            .period
            .as_ref()
            .filter(|period| period.basis == basis)?;
        Some((period.start.0, period.end_exclusive.map(|end| end.0)))
    }

    /// Whether this prefix's exact Hong Kong birth-registration period contains the date.
    pub fn supports_birth_registration_date(&self, date: NaiveDate) -> bool {
        self.birth_registration_period()
            .is_some_and(|(start, end)| start <= date && end.is_none_or(|end| date < end))
    }

    /// The prefix whose exact Hong Kong birth-registration period contains the date.
    pub fn from_birth_registration_date(date: NaiveDate) -> Option<&'static Self> {
        Self::all()
            .iter()
            .find(|prefix| prefix.supports_birth_registration_date(date))
    }

    /// Whether the exact first-issue period includes a day in the given calendar month.
    /// When supplied, `latest` limits eligible days to those on or before it.
    /// Out-of-range years or months have no match.
    pub fn supports_first_issue_month(
        &self,
        year: i32,
        month: u32,
        latest: Option<NaiveDate>,
    ) -> bool {
        NaiveDate::from_ymd_opt(year, month, 1)
            .is_some_and(|first| self.supports_first_issue_from(first, latest))
    }

    fn supports_first_issue_from(&self, first: NaiveDate, latest: Option<NaiveDate>) -> bool {
        let Some((start, end)) = self.first_issue_period() else {
            return false;
        };
        let earliest = first.max(start);
        earliest.year() == first.year()
            && earliest.month() == first.month()
            && end.is_none_or(|end| earliest < end)
            && latest.is_none_or(|limit| earliest <= limit)
    }

    /// All prefixes with an exact first-issue period overlapping the supplied month.
    pub fn from_first_issue_month(year: i32, month: u32) -> Vec<&'static Self> {
        Self::from_first_issue_month_between(year, month, None, None)
    }

    /// All first-issue prefixes eligible within the supplied month on or before `latest`.
    pub fn from_first_issue_month_through(
        year: i32,
        month: u32,
        latest: NaiveDate,
    ) -> Vec<&'static Self> {
        Self::from_first_issue_month_between(year, month, None, Some(latest))
    }

    /// Prefixes eligible in the month within the optional inclusive date bounds.
    pub fn from_first_issue_month_between(
        year: i32,
        month: u32,
        earliest: Option<NaiveDate>,
        latest: Option<NaiveDate>,
    ) -> Vec<&'static Self> {
        let Some(start) = NaiveDate::from_ymd_opt(year, month, 1) else {
            return Vec::new();
        };
        let first = earliest.map_or(start, |date| date.max(start));
        if (first.year(), first.month()) != (year, month) {
            return Vec::new();
        }
        Self::first_issue_prefixes_from(first, latest)
    }

    /// Prefixes eligible in `first`'s month, starting on that day and through `latest`.
    pub(crate) fn first_issue_prefixes_from(
        first: NaiveDate,
        latest: Option<NaiveDate>,
    ) -> Vec<&'static Self> {
        Self::all()
            .iter()
            .filter(|prefix| prefix.supports_first_issue_from(first, latest))
            .collect()
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Table {
    schema_version: u32,
    prefixes: Vec<Object<Entry>>,
}

#[derive(Debug, PartialEq, Eq, Deserialize)]
#[serde(deny_unknown_fields)]
struct Entry {
    id: String,
    descriptions: Object<Descriptions>,
    // The key is required even when its value is null.
    #[serde(deserialize_with = "Option::deserialize")]
    period: Option<Object<Period>>,
}

#[derive(Debug, PartialEq, Eq, Deserialize)]
#[serde(deny_unknown_fields)]
struct Descriptions {
    en: String,
    #[serde(rename = "zh-Hant")]
    zh_hant: String,
}

#[derive(Debug, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Period {
    basis: Basis,
    start: ReferenceDate,
    #[serde(deserialize_with = "Option::deserialize")]
    end_exclusive: Option<ReferenceDate>,
}

#[derive(Debug, PartialEq, Eq, Deserialize)]
#[serde(try_from = "String")]
enum Basis {
    BirthRegistration,
    FirstIssue,
}

impl TryFrom<String> for Basis {
    type Error = String;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        match value.as_str() {
            "BIRTH_REGISTRATION" => Ok(Self::BirthRegistration),
            "FIRST_ISSUE" => Ok(Self::FirstIssue),
            _ => Err(format!("unsupported prefix period basis {value:?}")),
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Deserialize)]
#[serde(try_from = "String")]
struct ReferenceDate(NaiveDate);

impl TryFrom<String> for ReferenceDate {
    type Error = String;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        let date: NaiveDate = value
            .parse()
            .map_err(|_| format!("invalid date {value:?}"))?;
        if date.to_string() != value {
            return Err(format!("expected an ISO calendar date, got {value:?}"));
        }
        Ok(Self(date))
    }
}

fn load_prefixes(input: &str) -> Result<Vec<DefinedPrefix>, String> {
    let Object(table): Object<Table> =
        serde_json::from_str(input).map_err(|error| error.to_string())?;
    if table.schema_version != 1 {
        return Err(format!(
            "unsupported prefix schema version {}",
            table.schema_version
        ));
    }
    let mut by_id = HashMap::new();
    for Object(entry) in table.prefixes {
        let id = entry.id.as_str();
        if !PREFIX_IDS.contains(&id) {
            return Err(format!("unknown prefix {id:?}"));
        }
        if entry.descriptions.en.is_empty() || entry.descriptions.zh_hant.is_empty() {
            return Err(format!("missing description for prefix {id}"));
        }
        if let Some(period) = &entry.period
            && period.end_exclusive.is_some_and(|end| end <= period.start)
        {
            return Err(format!("endExclusive must follow start for prefix {id}"));
        }
        if let Some(previous) = by_id.insert(entry.id.clone(), DefinedPrefix(entry)) {
            return Err(format!("duplicate prefix {}", previous.id()));
        }
    }
    PREFIX_IDS
        .iter()
        .map(|&id| {
            by_id
                .remove(id)
                .ok_or_else(|| format!("missing prefix {id}"))
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use serde_json::{Value, json};

    use super::*;

    #[test]
    fn lookup_normalizes_only_supported_letters_and_outer_whitespace() {
        let prefix = DefinedPrefix::lookup("\u{3000}ｗＸ\t").unwrap();
        assert_eq!(prefix.id(), "WX");
        assert!(!prefix.description().is_empty());
        assert!(!prefix.traditional_chinese_description().is_empty());
        assert_eq!(prefix.first_issue_period(), None);
        for input in ["W X", "ß", "Ｕ", "\u{200b}A", ""] {
            assert_eq!(DefinedPrefix::lookup(input), None, "{input:?}");
        }
    }

    #[test]
    fn first_issue_bounds_are_inclusive_and_confined_to_the_requested_month() {
        let matches = |date| {
            DefinedPrefix::from_first_issue_month_through(2026, 7, date)
                .into_iter()
                .map(DefinedPrefix::id)
                .collect::<Vec<_>>()
        };
        assert!(matches(NaiveDate::from_ymd_opt(2026, 6, 30).unwrap()).is_empty());
        assert_eq!(
            matches(NaiveDate::from_ymd_opt(2026, 7, 26).unwrap()),
            ["F"]
        );
        assert_eq!(
            matches(NaiveDate::from_ymd_opt(2026, 7, 27).unwrap()),
            ["F", "L"]
        );
        assert_eq!(
            matches(NaiveDate::from_ymd_opt(2026, 8, 1).unwrap()),
            ["F", "L"]
        );
        assert!(DefinedPrefix::from_first_issue_month(2026, 13).is_empty());
    }

    #[test]
    fn canonical_order_is_independent_of_data_row_order() {
        let mut data: Value = serde_json::from_str(PREFIX_DATA).unwrap();
        data["prefixes"].as_array_mut().unwrap().reverse();
        let result = load_prefixes(&data.to_string()).unwrap();
        assert_eq!(
            result.iter().map(DefinedPrefix::id).collect::<Vec<_>>(),
            PREFIX_IDS
        );
    }

    #[test]
    fn duplicate_prefix_ids_are_rejected_in_either_row_order() {
        let original: Value = serde_json::from_str(PREFIX_DATA).unwrap();
        for first_row in [0, 1] {
            let mut data = original.clone();
            let rows = data["prefixes"].as_array_mut().unwrap();
            rows.swap(0, first_row);
            let expected_error = format!("duplicate prefix {}", rows[0]["id"].as_str().unwrap());
            rows[1]["id"] = rows[0]["id"].clone();
            assert_eq!(load_prefixes(&data.to_string()).err(), Some(expected_error));
        }
    }

    #[test]
    fn rejects_invalid_schema_fields_keys_and_periods() {
        let original: Value = serde_json::from_str(PREFIX_DATA).unwrap();
        let index = original["prefixes"]
            .as_array()
            .unwrap()
            .iter()
            .position(|entry| entry["id"] == "F")
            .unwrap();
        let assert_rejected = |mutate: &dyn Fn(&mut Value)| {
            let mut data = original.clone();
            mutate(&mut data);
            assert!(load_prefixes(&data.to_string()).is_err());
        };
        assert_rejected(&|data| data["schemaVersion"] = json!(2));
        assert_rejected(&|data| data["schemaVersion"] = json!(1.5));
        assert_rejected(&|data| data["extra"] = Value::Null);
        assert_rejected(&|data| *data = json!([data["schemaVersion"], data["prefixes"]]));
        assert_rejected(&|data| {
            let row = &mut data["prefixes"][index];
            *row = json!([row["id"], row["descriptions"], row["period"]]);
        });
        assert_rejected(&|data| {
            let descriptions = &mut data["prefixes"][index]["descriptions"];
            *descriptions = json!([descriptions["en"], descriptions["zh-Hant"]]);
        });
        assert_rejected(&|data| {
            let period = &mut data["prefixes"][index]["period"];
            *period = json!([period["basis"], period["start"], period["endExclusive"]]);
        });
        assert_rejected(&|data| {
            data["prefixes"].as_array_mut().unwrap().pop();
        });
        assert_rejected(&|data| data["prefixes"][1]["id"] = json!("XX"));
        assert_rejected(&|data| data["prefixes"][0]["descriptions"]["zh-Hant"] = json!(""));
        assert_rejected(&|data| {
            data["prefixes"][0]
                .as_object_mut()
                .unwrap()
                .remove("period");
        });
        assert_rejected(&|data| {
            data["prefixes"][index]["period"]
                .as_object_mut()
                .unwrap()
                .remove("endExclusive");
        });
        for (key, value) in [
            ("basis", json!("UNKNOWN")),
            ("basis", json!({"FIRST_ISSUE": null})),
            ("start", json!("2020-02-31")),
            ("start", json!("2020-2-24")),
            ("endExclusive", json!("2020-02-24")),
        ] {
            assert_rejected(&|data| data["prefixes"][index]["period"][key] = value.clone());
        }
    }
}
