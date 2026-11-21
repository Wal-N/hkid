use hkid_utils::{HkidCard, HkidCardFields, NaiveDate, Sex};

fn check_summary(locale: &str, local_date: impl Fn() -> NaiveDate) {
    let today = local_date();
    // These birthdays catch a default date that is either a day early or a day late.
    for birthday in [today, today.succ_opt().unwrap()] {
        let card = HkidCard::new(HkidCardFields {
            date_of_birth: Some(birthday),
            sex: Some(Sex::Female),
            ..Default::default()
        })
        .unwrap();
        let before = local_date();
        let summary = card.describe_holder(None);
        let after = local_date();
        // Midnight may have passed while the system defaults were being read.
        assert!(
            [before, after]
                .into_iter()
                .any(|date| summary == card.describe_holder_on(Some(locale), date)),
            "unexpected summary for {birthday} in {locale}: {summary:?}"
        );
    }
}

#[cfg(target_os = "linux")]
fn main() {
    use chrono::{FixedOffset, Utc};

    for (zone, hours, locale, tag) in [
        ("UTC12", -12, "en_GB.UTF-8", "en-GB"),
        ("UTC-14", 14, "zh_HK.UTF-8", "zh-HK"),
        ("", 0, "en_GB.UTF-8", "en-GB"),
    ] {
        // SAFETY: Cargo runs this standalone test executable without the test harness.
        // It is single-threaded throughout, including all environment reads on Linux.
        unsafe {
            std::env::set_var("TZ", zone);
            std::env::set_var("LANGUAGE", locale);
        }
        let offset = FixedOffset::east_opt(hours * 3600).unwrap();
        check_summary(tag, || Utc::now().with_timezone(&offset).date_naive());
    }
}

#[cfg(not(target_os = "linux"))]
fn main() {
    let locale = sys_locale::get_locale().unwrap_or_else(|| "en".into());
    check_summary(&locale, || chrono::Local::now().date_naive());
}
