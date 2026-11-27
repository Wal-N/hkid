use hkid_utils::{GenerationError, HkidGenerator, HkidSymbol, NaiveDate, Sex};

pub fn check_generation_dates(local_date: impl Fn() -> NaiveDate) {
    let mut generator = HkidGenerator::new();
    for sex in [None, Some(Sex::Female), Some(Sex::Male)] {
        let mut saw_hong_kong = false;
        let mut saw_other_birthplace = false;
        for _ in 0..256 {
            let before = local_date();
            let card = match sex {
                None => generator.card(),
                Some(requested) => generator.card_with_sex(requested),
            }
            .unwrap();
            let after = local_date();
            assert!(card.fields().sex.is_some());
            assert!(sex.is_none_or(|requested| card.fields().sex == Some(requested)));
            // A call that crosses local midnight can use either date.
            assert!([before, after].into_iter().any(|date| {
                card.validate_as_of(date).is_ok()
                    && (11..=85).contains(&card.age_on(date).unwrap().unwrap())
            }));
            let born_in_hong_kong = card.fields().symbols.contains(HkidSymbol::BornInHongKong);
            saw_hong_kong |= born_in_hong_kong;
            saw_other_birthplace |= !born_in_hong_kong;
            if saw_hong_kong && saw_other_birthplace {
                break;
            }
        }
        assert!(saw_hong_kong, "expected a Hong Kong birthplace");
        assert!(
            saw_other_birthplace,
            "expected a birthplace outside Hong Kong"
        );
    }

    let today = local_date();
    for start in [today.pred_opt().unwrap(), today] {
        let before = local_date();
        let actual = generator.date_in_range(start, None);
        let after = local_date();
        assert!([before, after].into_iter().any(|end| match &actual {
            Ok(sample) => (start..end).contains(sample),
            Err(GenerationError::InvalidDateRange) => start >= end,
            Err(_) => false,
        }));
    }
}
