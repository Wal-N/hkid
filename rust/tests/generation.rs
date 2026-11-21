use std::collections::HashSet;

use chrono::{Datelike, Days, Months};
use hkid_utils::{
    DefinedPrefix, GenerationError, HkidCard, HkidGenerator, HkidNumber, HkidSymbol, NaiveDate,
    Sex, YearMonth,
};

fn date(year: i32, month: u32, day: u32) -> NaiveDate {
    NaiveDate::from_ymd_opt(year, month, day).unwrap()
}

fn assert_valid_number(number: &HkidNumber) {
    assert!((1..=2).contains(&number.prefix().len()));
    assert!(
        number
            .prefix()
            .bytes()
            .all(|byte| byte.is_ascii_uppercase())
    );
    assert_eq!(number.numerals().len(), 6);
    assert!(number.numerals().bytes().all(|byte| byte.is_ascii_digit()));
    assert_eq!(&number.to_string().parse::<HkidNumber>().unwrap(), number);
}

#[test]
fn number_generation_respects_each_prefix_policy() {
    let mut generator = HkidGenerator::with_seed(42);
    let allowed = [
        DefinedPrefix::lookup("F").unwrap(),
        DefinedPrefix::lookup("XA").unwrap(),
    ];
    assert_eq!(
        generator.number_from_prefixes(&[]),
        Err(GenerationError::EmptyPrefixes)
    );
    for prefix in allowed {
        let number = generator.number_from_prefixes(&[prefix]).unwrap();
        assert_eq!(number.defined_prefix(), Some(prefix));
    }

    let arbitrary: Vec<_> = (0..128)
        .map(|_| generator.number_with_any_prefix())
        .collect();
    for number in &arbitrary {
        assert_valid_number(number);
    }
    assert!(arbitrary.iter().any(|n| n.defined_prefix().is_none()));
    assert!(arbitrary.iter().any(|n| n.prefix().len() == 1));
    assert!(arbitrary.iter().any(|n| n.prefix().len() == 2));
    assert!(arbitrary.iter().any(|n| n.numerals().starts_with('0')));

    for _ in 0..128 {
        let defined = generator.number();
        assert_valid_number(&defined);
        assert!(defined.defined_prefix().is_some());

        let restricted = generator.number_from_prefixes(&allowed).unwrap();
        assert_valid_number(&restricted);
        assert!(allowed.contains(&restricted.defined_prefix().unwrap()));
    }
}

#[test]
fn date_ranges_are_half_open_across_the_supported_calendar() {
    let mut generator = HkidGenerator::with_seed(100);
    for start in [
        NaiveDate::MIN,
        date(2024, 2, 29),
        NaiveDate::MAX.pred_opt().unwrap(),
    ] {
        let end = start.succ_opt().unwrap();
        assert_eq!(generator.date_in_range(start, Some(end)).unwrap(), start);
        assert_eq!(
            generator.date_in_range(start, Some(start)),
            Err(GenerationError::InvalidDateRange)
        );
        assert_eq!(
            generator.date_in_range(end, Some(start)),
            Err(GenerationError::InvalidDateRange)
        );
    }
    for _ in 0..32 {
        let sampled = generator
            .date_in_range(NaiveDate::MIN, Some(NaiveDate::MAX))
            .unwrap();
        assert!((NaiveDate::MIN..NaiveDate::MAX).contains(&sampled));
    }
    let leap_dates: HashSet<_> = (0..32)
        .map(|_| {
            generator
                .date_in_range(date(2024, 2, 28), Some(date(2024, 3, 1)))
                .unwrap()
        })
        .collect();
    assert_eq!(
        leap_dates,
        HashSet::from([date(2024, 2, 28), date(2024, 2, 29)])
    );
}

#[test]
fn complete_cards_satisfy_the_reference_date_and_prefix_history() {
    let mut generator = HkidGenerator::with_seed(161);
    let start = HkidCard::SMART_CARD_START_DATE;
    assert_eq!(
        generator.card_on(start.pred_opt().unwrap(), None),
        Err(GenerationError::ReferenceBeforeSmartCard)
    );
    let mut generated_sexes = HashSet::new();
    for reference in [start, date(2024, 2, 29), date(2026, 7, 27), NaiveDate::MAX] {
        for requested_sex in [Some(Sex::Female), Some(Sex::Male), None] {
            for _ in 0..32 {
                let card = generator.card_on(reference, requested_sex).unwrap();
                assert_complete_card(&card, reference, requested_sex);
                if requested_sex.is_none() {
                    generated_sexes.insert(card.fields().sex.unwrap());
                }
            }
        }
    }
    assert_eq!(generated_sexes, HashSet::from([Sex::Male, Sex::Female]));
}

fn assert_complete_card(card: &HkidCard, reference: NaiveDate, requested_sex: Option<Sex>) {
    card.validate_as_of(reference).unwrap();
    assert!(card.find_suspicions().is_empty());

    let fields = card.fields();
    let sex = fields.sex.unwrap();
    assert!(requested_sex.is_none_or(|requested| requested == sex));
    let birth = fields.date_of_birth.unwrap();
    let registration = fields.date_of_registration.unwrap();
    let first_month = fields.first_registration_year_month.unwrap();
    let earliest = reference
        .checked_sub_months(Months::new(120))
        .unwrap()
        .max(HkidCard::SMART_CARD_START_DATE);
    assert!((earliest..=reference).contains(&registration));
    let eleventh_birthday = birth
        .with_year(birth.year() + 11)
        .unwrap_or_else(|| date(birth.year() + 11, 3, 1));
    let earliest_month = YearMonth::from(eleventh_birthday).max(YearMonth::new(1949, 1).unwrap());
    assert!((earliest_month..=YearMonth::from(registration)).contains(&first_month));
    let age = card.age_on(reference).unwrap().unwrap();
    assert!((11..=100).contains(&age));
    let permit = if age >= 18 {
        HkidSymbol::AdultReEntryPermit
    } else {
        HkidSymbol::MinorReEntryPermit
    };
    assert_eq!(
        fields.symbols.as_slice(),
        &[permit, HkidSymbol::RightOfAbode, HkidSymbol::BornInHongKong]
    );

    let number = fields.hkid_number.as_ref().unwrap();
    assert_valid_number(number);
    let prefix = number.defined_prefix().unwrap();
    if prefix.birth_registration_period().is_some() {
        assert!(
            (0..=42).any(|days| prefix.supports_birth_registration_date(
                birth.checked_add_days(Days::new(days)).unwrap()
            ))
        );
    } else {
        assert!(prefix.supports_first_issue_month(
            first_month.year(),
            first_month.month(),
            Some(registration)
        ));
    }

    let given_length = fields.chinese_name.personal_name().chars().count();
    assert!((1..=2).contains(&given_length));
    assert_eq!(fields.chinese_name.surname().chars().count(), 1);
    assert_eq!(
        fields.chinese_name.commercial_codes().len(),
        given_length + 1
    );
    assert!(!fields.english_name.surname().is_empty());
    assert_eq!(
        fields
            .english_name
            .personal_name()
            .split_whitespace()
            .count(),
        given_length
    );
}

#[test]
fn a_seed_reproduces_a_sequence_of_different_operations() {
    let reference = date(2028, 2, 29);
    let mut left = HkidGenerator::with_seed(82);
    let mut right = HkidGenerator::with_seed(82);
    for sex in [None, Some(Sex::Female), Some(Sex::Male)] {
        assert_eq!(
            left.number_with_any_prefix(),
            right.number_with_any_prefix()
        );
        let left_name = left.name(sex, Some(5)).unwrap();
        let right_name = right.name(sex, Some(5)).unwrap();
        assert_eq!(left_name.chinese.personal_name().chars().count(), 5);
        assert_eq!(left_name.chinese, right_name.chinese);
        assert_eq!(left_name.english, right_name.english);
        assert_eq!(
            left.date_in_range(NaiveDate::MIN, Some(reference)).unwrap(),
            right
                .date_in_range(NaiveDate::MIN, Some(reference))
                .unwrap()
        );
        assert_eq!(
            left.card_on(reference, sex).unwrap(),
            right.card_on(reference, sex).unwrap()
        );
    }
    let card = HkidGenerator::new().card_on(reference, None).unwrap();
    card.validate_as_of(reference).unwrap();
}
