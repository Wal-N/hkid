#[path = "common/commercial_code.rs"]
mod commercial_code;
mod common;

use std::{error::Error, fmt::Display, str::FromStr};

use chrono::NaiveDate;
use commercial_code::CodeInput;
use common::{Object, check_case_ids, check_schema_version, fixture_date};
use hkid_utils::{ChineseName, EnglishName, HkidCard, HkidCardFields, HkidNumber, YearMonth};
use serde::{Deserialize, Deserializer};

const CARD_FIXTURES: &str = include_str!("../../conformance/hkid-card.json");

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Fixtures {
    schema_version: u32,
    card_cases: Vec<Object<CardCase>>,
    age_cases: Vec<Object<AgeCase>>,
    invalid_card_cases: Vec<Object<InvalidCardCase>>,
    as_of_validation_cases: Vec<Object<AsOfCase>>,
    suspicion_cases: Vec<Object<SuspicionCase>>,
    holder_description_cases: Vec<Object<DescriptionCase>>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct CardInput {
    hkid_number: Option<String>,
    chinese_surname: Option<String>,
    chinese_personal_name: Option<String>,
    chinese_commercial_codes: Option<Vec<Object<CodeInput>>>,
    english_surname: Option<String>,
    english_personal_name: Option<String>,
    sex: Option<String>,
    #[serde(default, deserialize_with = "calendar_text::<NaiveDate, _>")]
    date_of_birth: Option<String>,
    symbols: Option<String>,
    #[serde(default, deserialize_with = "calendar_text::<YearMonth, _>")]
    first_registration_year_month: Option<String>,
    #[serde(default, deserialize_with = "calendar_text::<NaiveDate, _>")]
    date_of_registration: Option<String>,
}

impl CardInput {
    fn build(&self) -> Result<HkidCard, Box<dyn Error>> {
        let codes = self
            .chinese_commercial_codes
            .as_deref()
            .unwrap_or_default()
            .iter()
            .map(|code| code.build())
            .collect::<Result<Vec<_>, _>>()?;
        let fields = HkidCardFields {
            hkid_number: self.hkid_number.as_deref().map(str::parse).transpose()?,
            chinese_name: ChineseName::with_commercial_codes(
                self.chinese_surname.as_deref(),
                self.chinese_personal_name.as_deref(),
                codes,
            )?,
            english_name: EnglishName::new(
                self.english_surname.as_deref(),
                self.english_personal_name.as_deref(),
            )?,
            sex: self.sex.as_deref().map(str::parse).transpose()?,
            date_of_birth: self.date_of_birth.as_deref().map(str::parse).transpose()?,
            symbols: self.symbols.as_deref().unwrap_or_default().parse()?,
            first_registration_year_month: self
                .first_registration_year_month
                .as_deref()
                .map(str::parse)
                .transpose()?,
            date_of_registration: self
                .date_of_registration
                .as_deref()
                .map(str::parse)
                .transpose()?,
        };
        Ok(HkidCard::new(fields)?)
    }
}

// Empty text is an invalid card input. Other supplied strings must be ISO dates
// or months before an invalid-card case can exercise domain validation.
fn calendar_text<'de, T: FromStr + Display, D: Deserializer<'de>>(
    deserializer: D,
) -> Result<Option<String>, D::Error>
where
    T::Err: Display,
{
    let value = Option::<String>::deserialize(deserializer)?;
    if let Some(text) = value.as_deref().filter(|s| !s.is_empty()) {
        let parsed: T = text.parse().map_err(serde::de::Error::custom)?;
        if parsed.to_string() != text {
            return Err(serde::de::Error::custom(format!(
                "expected an ISO calendar value, received {text:?}"
            )));
        }
    }
    Ok(value)
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ExpectedCode {
    code: String,
    extension_number: Option<String>,
    display: String,
}

fn expected_codes<'de, D: Deserializer<'de>>(
    deserializer: D,
) -> Result<Option<Vec<Object<ExpectedCode>>>, D::Error> {
    Vec::deserialize(deserializer).map(Some)
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct ExpectedCard {
    #[serde(deserialize_with = "Option::deserialize")]
    hkid_without_check_digit: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    hkid_complete: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    hkid_masked: Option<String>,
    chinese_name: String,
    #[serde(default, deserialize_with = "expected_codes")]
    chinese_commercial_codes: Option<Vec<Object<ExpectedCode>>>,
    english_name: String,
    #[serde(deserialize_with = "Option::deserialize")]
    sex_printed_value: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    date_of_birth: Option<String>,
    symbols: String,
    #[serde(deserialize_with = "Option::deserialize")]
    first_registration_year_month: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    date_of_registration: Option<String>,
    #[serde(deserialize_with = "Option::deserialize")]
    age: Option<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct CardCase {
    id: String,
    input: Object<CardInput>,
    reference_date: String,
    expect: Object<ExpectedCard>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct AgeCase {
    id: String,
    date_of_birth: String,
    reference_date: String,
    expect_age: i32,
}

#[derive(Deserialize)]
struct InvalidCardCase {
    id: String,
    #[serde(flatten)]
    input: Object<CardInput>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AsOfCase {
    id: String,
    reference_date: String,
    expect_valid: bool,
    #[serde(flatten)]
    input: Object<CardInput>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct SuspicionCase {
    id: String,
    input: Object<CardInput>,
    expect_reasons: Vec<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct DescriptionCase {
    id: String,
    input: Object<CardInput>,
    reference_date: String,
    locale: Option<String>,
    default_locale: Option<String>,
    expect_description: String,
}

fn load_fixtures(input: &str) -> Result<Fixtures, String> {
    let Object(fixtures): Object<Fixtures> =
        serde_json::from_str(input).map_err(|e| e.to_string())?;
    check_schema_version(fixtures.schema_version)?;
    check_case_ids("cardCases", &fixtures.card_cases, |case| &case.id)?;
    check_case_ids("ageCases", &fixtures.age_cases, |case| &case.id)?;
    check_case_ids("invalidCardCases", &fixtures.invalid_card_cases, |case| {
        &case.id
    })?;
    check_case_ids(
        "asOfValidationCases",
        &fixtures.as_of_validation_cases,
        |case| &case.id,
    )?;
    check_case_ids("suspicionCases", &fixtures.suspicion_cases, |case| &case.id)?;
    check_case_ids(
        "holderDescriptionCases",
        &fixtures.holder_description_cases,
        |case| &case.id,
    )?;
    for case in &fixtures.holder_description_cases {
        if case.locale.is_none() && case.default_locale.is_none() {
            return Err(format!("{}: expected locale or defaultLocale", case.id));
        }
    }
    Ok(fixtures)
}

fn fixtures() -> Fixtures {
    load_fixtures(CARD_FIXTURES).expect("malformed hkid-card.json")
}

#[test]
fn formats_card_fields_and_preserves_commercial_codes() {
    let cases = fixtures().card_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let id = &case.id;
        let card = case.input.build().unwrap_or_else(|e| panic!("{id}: {e}"));
        let reference = fixture_date(id, &case.reference_date);
        let fields = card.fields();
        let number = fields.hkid_number.as_ref();
        let expected = &case.expect;
        assert_eq!(
            number.map(ToString::to_string),
            expected.hkid_complete,
            "{id}"
        );
        assert_eq!(
            number.map(HkidNumber::without_check_digit),
            expected.hkid_without_check_digit.as_deref(),
            "{id}"
        );
        assert_eq!(number.map(HkidNumber::masked), expected.hkid_masked, "{id}");
        assert_eq!(
            fields.chinese_name.to_string(),
            expected.chinese_name,
            "{id}"
        );
        assert_eq!(
            fields.english_name.to_string(),
            expected.english_name,
            "{id}"
        );
        assert_eq!(
            fields.sex.map(|sex| sex.to_string()),
            expected.sex_printed_value,
            "{id}"
        );
        assert_eq!(card.printed_date_of_birth(), expected.date_of_birth, "{id}");
        assert_eq!(fields.symbols.to_string(), expected.symbols, "{id}");
        assert_eq!(
            card.printed_first_registration_year_month(),
            expected.first_registration_year_month,
            "{id}"
        );
        assert_eq!(
            card.printed_date_of_registration(),
            expected.date_of_registration,
            "{id}"
        );
        assert_eq!(
            card.age_on(reference)
                .unwrap_or_else(|e| panic!("{id}: {e}")),
            expected.age,
            "{id}"
        );
        card.validate_as_of(reference)
            .unwrap_or_else(|e| panic!("{id}: {e}"));
        if let Some(codes) = &expected.chinese_commercial_codes {
            let actual = fields.chinese_name.commercial_codes();
            assert_eq!(actual.len(), codes.len(), "{id}");
            for (value, expected_code) in actual.iter().zip(codes) {
                assert_eq!(value.code(), expected_code.code, "{id}");
                assert_eq!(
                    value.extension_number(),
                    expected_code.extension_number.as_deref(),
                    "{id}"
                );
                assert_eq!(value.to_string(), expected_code.display, "{id}");
            }
        }
    }
}

#[test]
fn computes_age_on_the_reference_date() {
    let cases = fixtures().age_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let id = &case.id;
        let card = HkidCard::new(HkidCardFields {
            date_of_birth: Some(fixture_date(id, &case.date_of_birth)),
            ..Default::default()
        })
        .unwrap_or_else(|e| panic!("{id}: {e}"));
        let reference = fixture_date(id, &case.reference_date);
        assert_eq!(
            card.age_on(reference)
                .unwrap_or_else(|e| panic!("{id}: {e}")),
            Some(case.expect_age),
            "{id}"
        );
    }
}

#[test]
fn rejects_invalid_cards() {
    let cases = fixtures().invalid_card_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        assert!(case.input.build().is_err(), "{}", case.id);
    }
}

#[test]
fn validates_cards_as_of_the_reference_date() {
    let cases = fixtures().as_of_validation_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let id = &case.id;
        let card = case.input.build().unwrap_or_else(|e| panic!("{id}: {e}"));
        let reference = fixture_date(id, &case.reference_date);
        assert_eq!(
            card.validate_as_of(reference).is_ok(),
            case.expect_valid,
            "{id}"
        );
    }
}

#[test]
fn reports_distinct_suspicion_reasons_in_order() {
    let cases = fixtures().suspicion_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let id = &case.id;
        let card = case.input.build().unwrap_or_else(|e| panic!("{id}: {e}"));
        let reasons: Vec<_> = card
            .find_suspicions()
            .into_iter()
            .map(|reason| reason.as_str())
            .collect();
        assert_eq!(reasons, case.expect_reasons, "{id}");
        assert_eq!(
            card.is_suspicious(),
            !case.expect_reasons.is_empty(),
            "{id}"
        );
    }
}

#[test]
fn describes_holders_in_the_requested_or_default_language() {
    let cases = fixtures().holder_description_cases;
    assert!(!cases.is_empty());
    for Object(case) in cases {
        let id = &case.id;
        let card = case.input.build().unwrap_or_else(|e| panic!("{id}: {e}"));
        let reference = fixture_date(id, &case.reference_date);
        let language = case.locale.as_deref().or(case.default_locale.as_deref());
        let actual = card
            .describe_holder_on(language, reference)
            .unwrap_or_else(|e| panic!("{id}: {e}"));
        assert_eq!(actual, case.expect_description, "{id}");
    }
}

#[test]
fn rejects_malformed_card_fixtures_before_domain_validation() {
    use serde_json::{Value, json};

    let fixture: Value = serde_json::from_str(CARD_FIXTURES).unwrap();
    for group in ["invalidCardCases", "asOfValidationCases"] {
        let mut malformed = fixture.clone();
        malformed[group][0]["misspelledField"] = json!("unexpected");
        assert!(load_fixtures(&malformed.to_string()).is_err(), "{group}");
    }
    for value in [json!([null]), json!([{}]), json!([{"code": 1234}])] {
        let mut malformed = fixture.clone();
        malformed["invalidCardCases"][0]["chineseCommercialCodes"] = value;
        assert!(load_fixtures(&malformed.to_string()).is_err());
    }
    let mut malformed = fixture;
    let case = malformed["holderDescriptionCases"][0]
        .as_object_mut()
        .unwrap();
    case.remove("locale");
    case.remove("defaultLocale");
    assert!(load_fixtures(&malformed.to_string()).is_err());
}

#[test]
fn distinguishes_empty_calendar_inputs_from_malformed_fixtures() {
    use serde_json::{Value, json};

    let mut fixture: Value = serde_json::from_str(CARD_FIXTURES).unwrap();
    for (field, malformed) in [
        ("dateOfBirth", "2001-02-29"),
        ("dateOfBirth", "2001-2-28"),
        ("firstRegistrationYearMonth", "2020-13"),
        ("firstRegistrationYearMonth", "2020-6"),
        ("dateOfRegistration", "2020-02-31"),
    ] {
        fixture["invalidCardCases"] = json!([{"id": "calendar-input", (field): malformed}]);
        assert!(
            load_fixtures(&fixture.to_string()).is_err(),
            "{field}: {malformed}"
        );
        fixture["invalidCardCases"][0][field] = json!("");
        let cases = load_fixtures(&fixture.to_string())
            .unwrap()
            .invalid_card_cases;
        assert!(cases[0].input.build().is_err(), "{field}: empty string");
    }
}

#[test]
fn rejects_duplicate_ids_within_each_case_group() {
    common::assert_duplicate_ids_rejected(CARD_FIXTURES, load_fixtures);
}

#[test]
fn accepts_the_same_id_in_different_case_groups() {
    common::assert_cross_group_ids_accepted(CARD_FIXTURES, load_fixtures);
}
