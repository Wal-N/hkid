mod common;

use common::{Object, check_case_ids, check_schema_version, fixture_date};
use hkid_utils::HkidSymbols;
use serde::Deserialize;

const SYMBOL_FIXTURES: &str = include_str!("../../conformance/hkid-symbols.json");

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Fixtures {
    schema_version: u32,
    parse_cases: Vec<Object<ParseCase>>,
    invalid_parse_cases: Vec<Object<InvalidParseCase>>,
    age_validation_cases: Vec<Object<AgeValidationCase>>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ParseCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    input: Option<String>,
    expect_canonical: String,
}

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct InvalidParseCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    input: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct AgeValidationCase {
    id: String,
    symbols: String,
    date_of_birth: String,
    reference_date: String,
    expect_valid: bool,
}

fn load_fixtures(input: &str) -> Result<Fixtures, String> {
    let Object(fixtures): Object<Fixtures> =
        serde_json::from_str(input).map_err(|error| error.to_string())?;
    check_schema_version(fixtures.schema_version)?;
    check_case_ids("parseCases", &fixtures.parse_cases, |case| &case.id)?;
    check_case_ids("invalidParseCases", &fixtures.invalid_parse_cases, |case| {
        &case.id
    })?;
    check_case_ids(
        "ageValidationCases",
        &fixtures.age_validation_cases,
        |case| &case.id,
    )?;
    Ok(fixtures)
}

fn fixtures() -> Fixtures {
    load_fixtures(SYMBOL_FIXTURES).expect("malformed hkid-symbols.json")
}

#[test]
fn parses_and_formats_symbols_in_card_order() {
    let cases = fixtures().parse_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let input = case
            .input
            .as_deref()
            .unwrap_or_else(|| panic!("{}: null cannot be parsed", case.id));
        let symbols: HkidSymbols = input
            .parse()
            .unwrap_or_else(|err| panic!("{}: {err}", case.id));
        assert_eq!(symbols.to_string(), case.expect_canonical, "{}", case.id);
        assert!(HkidSymbols::is_valid(input), "{}", case.id);
    }
}

#[test]
fn rejects_invalid_symbol_strings() {
    let cases = fixtures().invalid_parse_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let input = case.input.as_deref();
        assert!(!input.is_some_and(HkidSymbols::is_valid), "{}", case.id);
        assert!(
            input.is_none_or(|value| value.parse::<HkidSymbols>().is_err()),
            "{}",
            case.id
        );
    }
}

#[test]
fn validates_symbols_against_age_at_registration() {
    let cases = fixtures().age_validation_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let symbols: HkidSymbols = case
            .symbols
            .parse()
            .unwrap_or_else(|err| panic!("{}: {err}", case.id));
        let birth = fixture_date(&case.id, &case.date_of_birth);
        let registration = fixture_date(&case.id, &case.reference_date);
        assert_eq!(
            symbols.validate_age(birth, registration).is_ok(),
            case.expect_valid,
            "{}",
            case.id
        );
    }
}

#[test]
fn requires_input_to_be_present_and_string_or_null() {
    let fixture: serde_json::Value = serde_json::from_str(SYMBOL_FIXTURES).unwrap();
    for group in ["parseCases", "invalidParseCases"] {
        let mut malformed = fixture.clone();
        malformed[group][0].as_object_mut().unwrap().remove("input");
        assert!(
            load_fixtures(&malformed.to_string()).is_err(),
            "{group}: missing input"
        );
        malformed[group][0]["input"] = serde_json::json!(false);
        assert!(
            load_fixtures(&malformed.to_string()).is_err(),
            "{group}: boolean input"
        );
        malformed[group][0]["input"] = serde_json::Value::Null;
        load_fixtures(&malformed.to_string()).expect("explicit null is a valid fixture input");
    }
}

#[test]
fn rejects_duplicate_ids_within_each_case_group() {
    common::assert_duplicate_ids_rejected(SYMBOL_FIXTURES, load_fixtures);
}

#[test]
fn accepts_the_same_id_in_different_case_groups() {
    common::assert_cross_group_ids_accepted(SYMBOL_FIXTURES, load_fixtures);
}
