use std::collections::HashSet;

use chrono::NaiveDate;
use serde_json::{Value, json};

#[path = "../../src/json_object.rs"]
mod json_object;
pub(crate) use json_object::Object;

pub(crate) fn check_schema_version(version: u32) -> Result<(), String> {
    if version != 1 {
        return Err(format!("unsupported fixture version {version}"));
    }
    Ok(())
}

pub(crate) fn check_case_ids<T>(
    group: &str,
    cases: &[Object<T>],
    case_id: impl Fn(&T) -> &str,
) -> Result<(), String> {
    let mut seen = HashSet::new();
    for case in cases {
        let id = case_id(case);
        if !seen.insert(id) {
            return Err(format!("duplicate case ID {id:?} in {group}"));
        }
    }
    Ok(())
}

pub(crate) fn fixture_date(id: &str, text: &str) -> NaiveDate {
    let date: NaiveDate = text.parse().unwrap_or_else(|err| panic!("{id}: {err}"));
    assert_eq!(
        date.to_string(),
        text,
        "{id}: expected an ISO calendar date"
    );
    date
}

pub(crate) fn assert_duplicate_ids_rejected<T>(input: &str, load: fn(&str) -> Result<T, String>) {
    let fixture: Value = serde_json::from_str(input).unwrap();
    for (group, value) in fixture.as_object().unwrap() {
        let Some(cases) = value.as_array() else {
            continue;
        };
        let id = cases[0]["id"].as_str().unwrap();
        let mut extra_case = cases.last().unwrap().clone();
        extra_case["id"] = json!(id);
        let mut duplicate = fixture.clone();
        duplicate[group].as_array_mut().unwrap().push(extra_case);
        assert_eq!(
            load(&duplicate.to_string()).err(),
            Some(format!("duplicate case ID {id:?} in {group}"))
        );
    }
}

pub(crate) fn assert_cross_group_ids_accepted<T>(input: &str, load: fn(&str) -> Result<T, String>) {
    let mut fixture: Value = serde_json::from_str(input).unwrap();
    for cases in fixture
        .as_object_mut()
        .unwrap()
        .values_mut()
        .filter_map(Value::as_array_mut)
    {
        cases.truncate(1);
        cases[0]["id"] = json!("shared-case-id");
    }
    load(&fixture.to_string()).expect("case IDs are scoped to their group");
}
