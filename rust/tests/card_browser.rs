#![cfg(all(target_arch = "wasm32", target_os = "unknown"))]

use hkid_utils::{HkidCard, HkidCardFields, HkidGenerator, NaiveDate, Sex};
use js_sys::Date;
use wasm_bindgen_test::{wasm_bindgen_test, wasm_bindgen_test_configure};

wasm_bindgen_test_configure!(run_in_browser);

#[path = "common/generation_defaults.rs"]
mod generation_defaults;

fn browser_date() -> NaiveDate {
    let now = Date::new_0();
    NaiveDate::from_ymd_opt(
        now.get_full_year() as i32,
        now.get_month() + 1,
        now.get_date(),
    )
    .unwrap()
}

#[wasm_bindgen_test]
fn uses_the_browsers_local_date() {
    let today = browser_date();
    for birthday in [today, today.succ_opt().unwrap()] {
        let card = HkidCard::new(HkidCardFields {
            date_of_birth: Some(birthday),
            ..Default::default()
        })
        .unwrap();
        let before = browser_date();
        let actual = card.describe_holder(Some("en"));
        let after = browser_date();
        // Include both dates in case the call straddled midnight.
        assert!(
            [before, after]
                .into_iter()
                .any(|date| actual == card.describe_holder_on(Some("en"), date)),
            "unexpected summary for {birthday}: {actual:?}"
        );
    }
}

// browser-webdriver.json configures both Firefox and Chrome to prefer zh-HK.
#[wasm_bindgen_test]
fn uses_the_browsers_preferred_language_unless_overridden() {
    let card = HkidCard::new(HkidCardFields {
        sex: Some(Sex::Female),
        ..Default::default()
    })
    .unwrap();
    let date = NaiveDate::from_ymd_opt(2026, 7, 21).unwrap();
    assert_eq!(
        card.describe_holder_on(None, date).unwrap(),
        "持有人為女性。"
    );
    assert_eq!(
        card.describe_holder_on(Some("en"), date).unwrap(),
        "The holder is female."
    );
}

#[wasm_bindgen_test]
fn generates_sample_cards_and_dates_in_the_browser() {
    generation_defaults::check_generation_dates(browser_date);
    let date = browser_date();
    let card = HkidGenerator::new().card_on(date, None).unwrap();
    assert!(card.validate_as_of(date).is_ok());
    assert!(!card.is_suspicious());
}
