use std::collections::HashSet;

use chrono::{Datelike, NaiveDate};
use hkid_utils::{DefinedPrefix, HkidNumber, ParseNumberError};
use serde::Deserialize;

#[path = "../src/json_object.rs"]
mod json_object;
use json_object::Object;

const NUMBER_FIXTURES: &str = include_str!("../../conformance/hkid-number.json");

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Fixtures {
    schema_version: u32,
    parse_cases: Vec<Object<ParseCase>>,
    invalid_parse_cases: Vec<Object<InvalidParseCase>>,
    check_digit_validation_cases: Vec<Object<CheckDigitCase>>,
    birth_registration_prefix_cases: Vec<Object<BirthPrefixCase>>,
    first_issue_prefix_cases: Vec<Object<FirstIssueCase>>,
}

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct ParseCase {
    id: String,
    // An explicit null is a domain input. A missing field is a fixture error.
    #[serde(deserialize_with = "Option::deserialize")]
    input: Option<String>,
    expect: Object<ExpectedNumber>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ExpectedNumber {
    prefix: String,
    numerals: String,
    check_digit: char,
    without_check_digit: String,
    without_parentheses: String,
    complete: String,
    masked: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct InvalidParseCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    input: Option<String>,
    expect_error: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct CheckDigitCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    input: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    check_digit: Option<String>,
    expect_valid: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct BirthPrefixCase {
    id: String,
    date: String,
    #[serde(deserialize_with = "Option::deserialize")]
    expect_prefix: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct FirstIssueCase {
    id: String,
    month: String,
    expect_prefixes: Vec<String>,
}

fn check_case_ids<T>(
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

fn load_fixtures(input: &str) -> Result<Fixtures, String> {
    let Object(fixtures): Object<Fixtures> =
        serde_json::from_str(input).map_err(|error| error.to_string())?;
    if fixtures.schema_version != 1 {
        return Err(format!(
            "unsupported fixture version {}",
            fixtures.schema_version
        ));
    }
    check_case_ids("parseCases", &fixtures.parse_cases, |case| &case.id)?;
    check_case_ids("invalidParseCases", &fixtures.invalid_parse_cases, |case| {
        &case.id
    })?;
    check_case_ids(
        "checkDigitValidationCases",
        &fixtures.check_digit_validation_cases,
        |case| &case.id,
    )?;
    check_case_ids(
        "birthRegistrationPrefixCases",
        &fixtures.birth_registration_prefix_cases,
        |case| &case.id,
    )?;
    check_case_ids(
        "firstIssuePrefixCases",
        &fixtures.first_issue_prefix_cases,
        |case| &case.id,
    )?;
    Ok(fixtures)
}

fn fixtures() -> Fixtures {
    load_fixtures(NUMBER_FIXTURES).expect("malformed hkid-number.json")
}

#[test]
fn parses_and_formats_numbers() {
    let cases = fixtures().parse_cases;
    assert!(!cases.is_empty());
    for Object(ParseCase {
        id,
        input,
        expect: Object(expect),
    }) in cases
    {
        let input = input
            .as_deref()
            .unwrap_or_else(|| panic!("{id}: null cannot be parsed"));
        assert!(HkidNumber::is_valid(input), "{id}");
        let number: HkidNumber = input.parse().unwrap_or_else(|err| panic!("{id}: {err}"));
        assert_eq!(number.prefix(), expect.prefix, "{id}");
        assert_eq!(number.numerals(), expect.numerals, "{id}");
        assert_eq!(number.check_digit(), expect.check_digit, "{id}");
        assert_eq!(
            number.without_check_digit(),
            expect.without_check_digit,
            "{id}"
        );
        assert_eq!(
            number.without_parentheses(),
            expect.without_parentheses,
            "{id}"
        );
        assert_eq!(number.to_string(), expect.complete, "{id}");
        assert_eq!(number.masked(), expect.masked, "{id}");
    }
}

#[test]
fn rejects_invalid_numbers_with_the_expected_error() {
    let cases = fixtures().invalid_parse_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let id = case.id;
        let input = case.input.as_deref();
        let expected = match case.expect_error.as_str() {
            "INVALID_FORMAT" => ParseNumberError::InvalidFormat,
            "INVALID_CHECK_DIGIT" => ParseNumberError::InvalidCheckDigit,
            other => panic!("{id}: unsupported error category {other}"),
        };
        assert!(!input.is_some_and(HkidNumber::is_valid), "{id}");
        let parsed = input
            .ok_or(ParseNumberError::InvalidFormat)
            .and_then(str::parse::<HkidNumber>);
        assert_eq!(parsed, Err(expected), "{id}");
    }
}

#[test]
fn validates_separate_check_digits() {
    let cases = fixtures().check_digit_validation_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let valid = case
            .input
            .as_deref()
            .zip(case.check_digit.as_deref())
            .is_some_and(|(body, digit)| HkidNumber::is_valid_check_digit(body, digit));
        assert_eq!(valid, case.expect_valid, "{}", case.id);
    }
}

fn fixture_date(id: &str, text: &str) -> NaiveDate {
    let date: NaiveDate = text.parse().unwrap_or_else(|err| panic!("{id}: {err}"));
    assert_eq!(
        date.to_string(),
        text,
        "{id}: expected an ISO calendar date"
    );
    date
}

#[test]
fn resolves_birth_registration_prefixes() {
    let cases = fixtures().birth_registration_prefix_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let date = fixture_date(&case.id, &case.date);
        let prefix = DefinedPrefix::from_birth_registration_date(date).map(DefinedPrefix::id);
        assert_eq!(prefix, case.expect_prefix.as_deref(), "{}", case.id);
    }
}

#[test]
fn resolves_first_issue_prefixes_in_order() {
    let cases = fixtures().first_issue_prefix_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let first_day = fixture_date(&case.id, &format!("{}-01", case.month));
        let prefixes: Vec<_> =
            DefinedPrefix::from_first_issue_month(first_day.year(), first_day.month())
                .into_iter()
                .map(DefinedPrefix::id)
                .collect();
        assert_eq!(prefixes, case.expect_prefixes, "{}", case.id);
    }
}

#[test]
fn requires_explicit_strings_or_nulls_for_validation_inputs() {
    let nulls = r#"{"id":"nullable","input":null,"checkDigit":null,"expectValid":false}"#;
    let Object(case): Object<CheckDigitCase> = serde_json::from_str(nulls).unwrap();
    assert!(case.input.is_none() && case.check_digit.is_none());
    for malformed in [
        r#"{"id":"missing-body","checkDigit":"3","expectValid":false}"#,
        r#"{"id":"missing-digit","input":"A123456","expectValid":false}"#,
        r#"{"id":"wrong-type","input":"A123456","checkDigit":3,"expectValid":false}"#,
    ] {
        assert!(
            serde_json::from_str::<Object<CheckDigitCase>>(malformed).is_err(),
            "{malformed}"
        );
    }
}

#[test]
fn requires_json_objects_for_the_document_cases_and_expectations() {
    let array_root = "[1, [], [], [], [], []]";
    assert!(serde_json::from_str::<Object<Fixtures>>(array_root).is_err());

    let array_case = r#"["nullable",null,null,false]"#;
    assert!(serde_json::from_str::<Object<CheckDigitCase>>(array_case).is_err());

    let array_expectation = r#"{
        "id":"array-expectation", "input":"A123456",
        "expect":["A","123456","3","A123456","A1234563","A123456(3)","****456(*)"]
    }"#;
    assert!(serde_json::from_str::<Object<ParseCase>>(array_expectation).is_err());
}

#[test]
fn rejects_duplicate_ids_within_each_case_group() {
    let fixture: serde_json::Value = serde_json::from_str(NUMBER_FIXTURES).unwrap();
    for (group, value) in fixture.as_object().unwrap() {
        let Some(cases) = value.as_array() else {
            continue;
        };
        let id = cases[0]["id"].as_str().unwrap();
        let mut extra_case = cases.last().unwrap().clone();
        extra_case["id"] = serde_json::json!(id);
        let mut duplicate = fixture.clone();
        duplicate[group].as_array_mut().unwrap().push(extra_case);
        assert_eq!(
            load_fixtures(&duplicate.to_string()).err(),
            Some(format!("duplicate case ID {id:?} in {group}"))
        );
    }
}

#[test]
fn accepts_the_same_id_in_different_case_groups() {
    let mut fixture: serde_json::Value = serde_json::from_str(NUMBER_FIXTURES).unwrap();
    for cases in fixture
        .as_object_mut()
        .unwrap()
        .values_mut()
        .filter_map(serde_json::Value::as_array_mut)
    {
        cases.truncate(1);
        cases[0]["id"] = serde_json::json!("shared-case-id");
    }
    load_fixtures(&fixture.to_string()).expect("case IDs are scoped to their group");
}
