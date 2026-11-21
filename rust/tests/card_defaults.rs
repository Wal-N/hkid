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
fn check_linux_summary(zone: impl AsRef<std::ffi::OsStr>, hours: i32, locale: &str, tag: &str) {
    use chrono::{FixedOffset, Utc};

    // SAFETY: Cargo runs this standalone test executable without the test harness.
    // It is single-threaded throughout, including all environment reads on Linux.
    unsafe {
        std::env::set_var("TZ", zone);
        std::env::set_var("LANGUAGE", locale);
    }
    let offset = FixedOffset::east_opt(hours * 3600).unwrap();
    check_summary(tag, || Utc::now().with_timezone(&offset).date_naive());
}

#[cfg(all(target_os = "linux", target_env = "gnu"))]
fn check_glibc_tzdir() {
    use std::{fs, path::Path};

    let dir = Path::new(env!("CARGO_TARGET_TMPDIR")).join(format!("tzdir-{}", std::process::id()));
    fs::create_dir(&dir).unwrap();
    // Valid TZif v1 files with a single fixed-offset type and no transitions.
    // Override two names that would otherwise both select UTC. The two local
    // dates differ at every time of day, so ignoring TZDIR always fails.
    for (name, hours) in [("UTC", -12_i32), ("GMT", 14)] {
        let mut tzif = b"TZif".to_vec();
        tzif.resize(36, 0);
        tzif.extend(1_u32.to_be_bytes()); // local time type count
        tzif.extend(4_u32.to_be_bytes()); // designation length
        tzif.extend((hours * 3600).to_be_bytes());
        tzif.extend(b"\0\0TST\0"); // standard time, designation at index zero
        fs::write(dir.join(name), tzif).unwrap();
    }
    // SAFETY: This test runs in the single-threaded executable described above.
    unsafe { std::env::set_var("TZDIR", &dir) };
    check_linux_summary("UTC", -12, "en_GB.UTF-8", "en-GB");
    check_linux_summary(":GMT", 14, "en_GB.UTF-8", "en-GB");
    check_linux_summary(dir.join("GMT"), 14, "en_GB.UTF-8", "en-GB");
    check_linux_summary("UTC12", -12, "en_GB.UTF-8", "en-GB");
    // These rules omit DST transition dates. glibc supplies them; equal
    // standard and daylight offsets keep the expected date valid year-round.
    check_linux_summary("AAA12BBB12", -12, "en_GB.UTF-8", "en-GB");
    check_linux_summary("AAA-14BBB-14", 14, "en_GB.UTF-8", "en-GB");
    fs::remove_file(dir.join("UTC")).unwrap();
    fs::remove_file(dir.join("GMT")).unwrap();
    fs::remove_dir(dir).unwrap();
}

#[cfg(target_os = "linux")]
fn main() {
    for (zone, hours, locale, tag) in [
        ("UTC12", -12, "en_GB.UTF-8", "en-GB"),
        ("UTC-14", 14, "zh_HK.UTF-8", "zh-HK"),
        ("", 0, "en_GB.UTF-8", "en-GB"),
    ] {
        check_linux_summary(zone, hours, locale, tag);
    }
    #[cfg(target_env = "gnu")]
    check_glibc_tzdir();
}

#[cfg(not(target_os = "linux"))]
fn main() {
    let locale = sys_locale::get_locale().unwrap_or_else(|| "en".into());
    check_summary(&locale, || chrono::Local::now().date_naive());
}
