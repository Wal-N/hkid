use hkid_utils::{HkidGenerator, NaiveDate, Sex};

pub fn check_generation_dates(local_date: impl Fn() -> NaiveDate) {
    for sex in [None, Some(Sex::Female)] {
        let before = local_date();
        let actual = HkidGenerator::with_seed(42).card(sex);
        let after = local_date();
        // A call that crosses local midnight can use either date.
        assert!(
            [before, after]
                .into_iter()
                .any(|date| { actual == HkidGenerator::with_seed(42).card_on(date, sex) })
        );
    }

    let today = local_date();
    for start in [today.pred_opt().unwrap(), today] {
        let before = local_date();
        let actual = HkidGenerator::with_seed(42).date_in_range(start, None);
        let after = local_date();
        assert!([before, after].into_iter().any(|date| {
            actual == HkidGenerator::with_seed(42).date_in_range(start, Some(date))
        }));
    }
}
