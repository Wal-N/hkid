use chrono::{Datelike, Days, Months};
use hkid_utils::{
    ChineseNameEntry, DefinedPrefix, GenerationError, HkidCard, HkidGenerator, HkidNumber,
    HkidSymbol, HkidSymbolCategory, NaiveDate, Sex, YearMonth,
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
    assert_eq!(
        &HkidNumber::parse_with_check_digit(&number.to_string()).unwrap(),
        number
    );
}

#[test]
fn exposes_the_bundled_name_seed_to_callers() {
    let entries = ChineseNameEntry::all().unwrap();
    let find = |character| {
        entries
            .iter()
            .find(|entry| entry.character() == character)
            .unwrap()
    };

    let chan = find('陳');
    assert_eq!(chan.commercial_code().code(), "7115");
    assert_eq!(chan.romanisation(), "Chan");
    assert!(chan.is_common_surname());
    assert_eq!(chan.weight(), 100);
    assert_eq!(chan.supported_sexes(), &[Sex::Male, Sex::Female]);
    assert!(chan.supports_sex(Sex::Male));
    assert!(chan.supports_sex(Sex::Female));

    let chun = find('俊');
    assert_eq!(chun.commercial_code().code(), "0193");
    assert!(!chun.is_common_surname());
    assert_eq!(chun.supported_sexes(), &[Sex::Male]);
    assert!(!chun.supports_sex(Sex::Female));

    let yin = find('妍');
    assert_eq!(yin.supported_sexes(), &[Sex::Female]);
    assert!(!yin.supports_sex(Sex::Male));
}

#[test]
fn number_generation_uses_defined_or_caller_selected_prefixes() {
    let mut generator = HkidGenerator::new();
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
fn arbitrary_prefix_generation_covers_both_lengths_and_unlisted_prefixes() {
    let mut generator = HkidGenerator::new();
    let mut saw_one_letter = false;
    let mut saw_two_letters = false;
    let mut saw_unlisted = false;
    let mut saw_leading_zero = false;

    for _ in 0..1024 {
        let number = generator.number_with_any_prefix();
        assert_valid_number(&number);
        saw_one_letter |= number.prefix().len() == 1;
        saw_two_letters |= number.prefix().len() == 2;
        saw_unlisted |= number.defined_prefix().is_none();
        saw_leading_zero |= number.numerals().starts_with('0');
        if saw_one_letter && saw_two_letters && saw_unlisted && saw_leading_zero {
            break;
        }
    }

    assert!(saw_one_letter, "no one-letter prefix generated");
    assert!(saw_two_letters, "no two-letter prefix generated");
    assert!(saw_unlisted, "generated only predefined prefixes");
    assert!(saw_leading_zero, "no six-digit body with a leading zero");
}

#[test]
fn date_ranges_are_half_open_across_the_supported_calendar() {
    let mut generator = HkidGenerator::new();
    for start in [
        NaiveDate::MIN,
        date(2024, 2, 28),
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
}

#[test]
fn complete_cards_satisfy_the_reference_date_and_prefix_history() {
    let mut generator = HkidGenerator::new();
    let start = HkidCard::SMART_CARD_START_DATE;
    assert_eq!(
        generator.card_on(start.pred_opt().unwrap(), None),
        Err(GenerationError::ReferenceBeforeSmartCard)
    );
    for reference in [start, date(2024, 2, 29), date(2026, 7, 27), NaiveDate::MAX] {
        for requested_sex in [Some(Sex::Female), Some(Sex::Male), None] {
            for _ in 0..32 {
                let card = generator.card_on(reference, requested_sex).unwrap();
                assert_complete_card(&card, reference, requested_sex);
            }
        }
    }
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
    assert!((11..=85).contains(&age));
    let permit = if age >= 18 {
        HkidSymbol::AdultReEntryPermit
    } else {
        HkidSymbol::MinorReEntryPermit
    };
    let birthplace = fields
        .symbols
        .by_category(HkidSymbolCategory::ReportedPlaceOfBirth)
        .next()
        .expect("generated cards must report a birthplace");
    assert_eq!(
        fields.symbols.as_slice(),
        &[permit, HkidSymbol::RightOfAbode, birthplace]
    );

    let number = fields.hkid_number.as_ref().unwrap();
    assert_valid_number(number);
    let prefix = number.defined_prefix().unwrap();
    if prefix.birth_registration_period().is_some() {
        assert_eq!(birthplace, HkidSymbol::BornInHongKong);
        assert!(
            (0..=42).any(|days| prefix.supports_birth_registration_date(
                birth.checked_add_days(Days::new(days)).unwrap()
            ))
        );
    } else {
        if birthplace == HkidSymbol::BornInHongKong {
            assert!((0..=42).any(|days| {
                DefinedPrefix::from_birth_registration_date(birth + Days::new(days)).is_none()
            }));
        }
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
