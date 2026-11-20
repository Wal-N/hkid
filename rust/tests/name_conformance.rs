mod common;

use common::{Object, check_case_ids, check_schema_version};
use hkid_utils::{ChineseCommercialCode, ChineseName, EnglishName};
use serde::{Deserialize, Deserializer};

const NAME_FIXTURES: &str = include_str!("../../conformance/hkid-names.json");

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Fixtures {
    schema_version: u32,
    commercial_code_cases: Vec<Object<CodeCase>>,
    invalid_commercial_code_cases: Vec<Object<InvalidCodeCase>>,
    commercial_code_comparison_cases: Vec<Object<CodeComparisonCase>>,
    chinese_name_cases: Vec<Object<ChineseNameCase>>,
    invalid_chinese_name_cases: Vec<Object<InvalidChineseNameCase>>,
    english_name_validation_cases: Vec<Object<EnglishValidationCase>>,
    english_name_cases: Vec<Object<EnglishNameCase>>,
    invalid_english_name_cases: Vec<Object<InvalidEnglishNameCase>>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct CodeInput {
    #[serde(deserialize_with = "Option::deserialize")]
    code: Option<String>,
    extension_number: Option<String>,
}

impl CodeInput {
    fn build(&self) -> Result<ChineseCommercialCode, String> {
        let code = self.code.as_deref().ok_or("null main code")?;
        ChineseCommercialCode::new(code, self.extension_number.as_deref())
            .map_err(|error| error.to_string())
    }
}

// An optional expectation can be omitted, but an explicit value must be the expected type.
fn when_present<'de, D: Deserializer<'de>, T: Deserialize<'de>>(
    deserializer: D,
) -> Result<Option<T>, D::Error> {
    T::deserialize(deserializer).map(Some)
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ExpectedCode {
    code: String,
    extension_number: Option<String>,
    #[serde(default, deserialize_with = "when_present")]
    display: Option<String>,
}

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct CodeCase {
    id: String,
    input: Object<CodeInput>,
    expect: Object<ExpectedCode>,
}

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct InvalidCodeCase {
    id: String,
    input: Object<CodeInput>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct CodeComparisonCase {
    id: String,
    left: Object<CodeInput>,
    right: Object<CodeInput>,
    expect_same_value: bool,
}

type CodeInputs = Vec<Object<CodeInput>>;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ChineseNameCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    surname: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    personal_name: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    commercial_codes: Option<CodeInputs>,
    expect_full_name: String,
    #[serde(default, deserialize_with = "when_present")]
    expect_commercial_codes: Option<Vec<Object<ExpectedCode>>>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct InvalidChineseNameCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    surname: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    personal_name: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    commercial_codes: Option<CodeInputs>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct EnglishValidationCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    surname: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    personal_name: Option<String>,
    expect_valid: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct EnglishNameCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    surname: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    personal_name: Option<String>,
    #[serde(default, deserialize_with = "when_present")]
    expect_surname: Option<String>,
    #[serde(default, deserialize_with = "when_present")]
    expect_personal_name: Option<String>,
    expect_full_name: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct InvalidEnglishNameCase {
    id: String,
    #[serde(deserialize_with = "Option::deserialize")]
    surname: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    personal_name: Option<String>,
}

fn load_fixtures(input: &str) -> Result<Fixtures, String> {
    let Object(fixtures): Object<Fixtures> =
        serde_json::from_str(input).map_err(|error| error.to_string())?;
    check_schema_version(fixtures.schema_version)?;
    check_case_ids(
        "commercialCodeCases",
        &fixtures.commercial_code_cases,
        |c| &c.id,
    )?;
    check_case_ids(
        "invalidCommercialCodeCases",
        &fixtures.invalid_commercial_code_cases,
        |c| &c.id,
    )?;
    check_case_ids(
        "commercialCodeComparisonCases",
        &fixtures.commercial_code_comparison_cases,
        |c| &c.id,
    )?;
    check_case_ids("chineseNameCases", &fixtures.chinese_name_cases, |c| &c.id)?;
    check_case_ids(
        "invalidChineseNameCases",
        &fixtures.invalid_chinese_name_cases,
        |c| &c.id,
    )?;
    check_case_ids(
        "englishNameValidationCases",
        &fixtures.english_name_validation_cases,
        |c| &c.id,
    )?;
    check_case_ids("englishNameCases", &fixtures.english_name_cases, |c| &c.id)?;
    check_case_ids(
        "invalidEnglishNameCases",
        &fixtures.invalid_english_name_cases,
        |c| &c.id,
    )?;
    Ok(fixtures)
}

fn fixtures() -> Fixtures {
    load_fixtures(NAME_FIXTURES).expect("malformed hkid-names.json")
}

fn assert_code(
    id: &str,
    actual: &ChineseCommercialCode,
    code: &str,
    extension: Option<&str>,
    display: &str,
) {
    assert_eq!(actual.code(), code, "{id}");
    assert_eq!(actual.extension_number(), extension, "{id}");
    assert_eq!(actual.to_string(), display, "{id}");
}

#[test]
fn preserves_commercial_code_values_and_display() {
    let cases = fixtures().commercial_code_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let code = case
            .input
            .build()
            .unwrap_or_else(|err| panic!("{}: {err}", case.id));
        let expected = &case.expect;
        let display = expected
            .display
            .as_deref()
            .unwrap_or_else(|| panic!("{}: missing display", case.id));
        assert_code(
            &case.id,
            &code,
            &expected.code,
            expected.extension_number.as_deref(),
            display,
        );
    }
}

#[test]
fn rejects_invalid_commercial_codes() {
    let cases = fixtures().invalid_commercial_code_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        assert!(case.input.build().is_err(), "{}", case.id);
    }
}

#[test]
fn compares_complete_commercial_code_values() {
    let cases = fixtures().commercial_code_comparison_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let left = case
            .left
            .build()
            .unwrap_or_else(|err| panic!("{}: {err}", case.id));
        let right = case
            .right
            .build()
            .unwrap_or_else(|err| panic!("{}: {err}", case.id));
        assert_eq!(left == right, case.expect_same_value, "{}", case.id);
    }
}

fn build_chinese_name(
    surname: Option<&str>,
    personal_name: Option<&str>,
    inputs: &[Object<CodeInput>],
) -> Result<ChineseName, String> {
    let codes = inputs
        .iter()
        .map(|input| input.build())
        .collect::<Result<_, _>>()?;
    ChineseName::with_commercial_codes(surname, personal_name, codes)
        .map_err(|error| error.to_string())
}

#[test]
fn constructs_chinese_names_and_preserves_printed_code_order() {
    let cases = fixtures().chinese_name_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let surname = case.surname.as_deref();
        let personal = case.personal_name.as_deref();
        let inputs = case.commercial_codes.as_deref().unwrap_or_default();
        let name = build_chinese_name(surname, personal, inputs)
            .unwrap_or_else(|err| panic!("{}: {err}", case.id));
        assert_eq!(name.surname(), surname.unwrap_or_default(), "{}", case.id);
        assert_eq!(
            name.personal_name(),
            personal.unwrap_or_default(),
            "{}",
            case.id
        );
        assert_eq!(name.to_string(), case.expect_full_name, "{}", case.id);
        let codes = name.commercial_codes();
        if let Some(expected) = &case.expect_commercial_codes {
            assert_eq!(codes.len(), expected.len(), "{}", case.id);
            for (actual, value) in codes.iter().zip(expected) {
                assert_code(
                    &case.id,
                    actual,
                    &value.code,
                    value.extension_number.as_deref(),
                    value.display.as_deref().unwrap_or(&value.code),
                );
            }
        } else {
            assert_eq!(codes.len(), inputs.len(), "{}", case.id);
            for (actual, input) in codes.iter().zip(inputs) {
                let code = input
                    .code
                    .as_deref()
                    .expect("accepted main code must be present");
                assert_code(
                    &case.id,
                    actual,
                    code,
                    input.extension_number.as_deref(),
                    code,
                );
            }
        }
    }
}

#[test]
fn rejects_invalid_chinese_names_and_code_collections() {
    let cases = fixtures().invalid_chinese_name_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        assert!(
            build_chinese_name(
                case.surname.as_deref(),
                case.personal_name.as_deref(),
                case.commercial_codes.as_deref().unwrap_or_default()
            )
            .is_err(),
            "{}",
            case.id
        );
    }
}

#[test]
fn validates_complete_english_names() {
    let cases = fixtures().english_name_validation_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        assert_eq!(
            EnglishName::is_valid(case.surname.as_deref(), case.personal_name.as_deref()),
            case.expect_valid,
            "{}",
            case.id
        );
    }
}

#[test]
fn constructs_and_formats_english_names() {
    let cases = fixtures().english_name_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let surname = case.surname.as_deref();
        let personal = case.personal_name.as_deref();
        let name =
            EnglishName::new(surname, personal).unwrap_or_else(|err| panic!("{}: {err}", case.id));
        // A null input needs an explicit expected string, even when that string is empty.
        assert_eq!(
            Some(name.surname()),
            case.expect_surname.as_deref().or(surname),
            "{}",
            case.id
        );
        assert_eq!(
            Some(name.personal_name()),
            case.expect_personal_name.as_deref().or(personal),
            "{}",
            case.id
        );
        assert_eq!(name.to_string(), case.expect_full_name, "{}", case.id);
    }
}

#[test]
fn rejects_invalid_english_names() {
    let cases = fixtures().invalid_english_name_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        assert!(
            EnglishName::new(case.surname.as_deref(), case.personal_name.as_deref()).is_err(),
            "{}",
            case.id
        );
    }
}

#[test]
fn rejects_malformed_name_parts_and_code_inputs() {
    use serde_json::{Value, json};
    let fixture: Value = serde_json::from_str(NAME_FIXTURES).unwrap();
    for group in ["englishNameCases", "invalidEnglishNameCases"] {
        for part in ["surname", "personalName"] {
            let mut invalid = fixture.clone();
            invalid[group][0].as_object_mut().unwrap().remove(part);
            assert!(
                load_fixtures(&invalid.to_string()).is_err(),
                "{group}: missing {part}"
            );
            invalid[group][0][part] = json!(42);
            assert!(
                load_fixtures(&invalid.to_string()).is_err(),
                "{group}: numeric {part}"
            );
        }
    }
    let mut invalid = fixture.clone();
    invalid["invalidChineseNameCases"][0]["commercialCodes"] = json!([null]);
    assert!(load_fixtures(&invalid.to_string()).is_err());
    let mut invalid = fixture;
    for malformed in [
        json!({}),
        json!({"code": 1234}),
        json!({"code": "0123", "extensionNumber": 2}),
        json!(["0123", null]),
    ] {
        invalid["invalidCommercialCodeCases"][0]["input"] = malformed;
        assert!(load_fixtures(&invalid.to_string()).is_err());
    }
}

#[test]
fn rejects_duplicate_ids_within_each_case_group() {
    common::assert_duplicate_ids_rejected(NAME_FIXTURES, load_fixtures);
}

#[test]
fn accepts_the_same_id_in_different_case_groups() {
    common::assert_cross_group_ids_accepted(NAME_FIXTURES, load_fixtures);
}
