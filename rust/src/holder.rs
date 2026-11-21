//! Advisory consistency checks and localized holder descriptions.

use chrono::{Local, NaiveDate};
#[cfg(not(all(target_arch = "wasm32", target_os = "unknown")))]
use sys_locale::get_locale as system_locale;

use crate::card::{CardError, HkidCard, Sex};
use crate::{HkidNumber, HkidSymbol, HkidSymbolCategory, HkidSymbols};

/// A potentially inconsistent combination of supplied fields, in reporting order.
#[derive(Clone, Copy, Debug, Eq, Hash, Ord, PartialEq, PartialOrd)]
pub enum HkidSuspicion {
    /// The birth date is on or after the end of the prefix's birth-registration period.
    PrefixBirthDateMismatch,
    /// A Hong Kong birth-registration prefix accompanies a reported birthplace outside Hong Kong.
    PrefixBirthPlaceMismatch,
    /// The first-registration month falls outside the known first-issue period.
    PrefixFirstRegistrationMismatch,
}

impl HkidSuspicion {
    /// Stable identifier used by the shared card fixtures.
    pub fn as_str(self) -> &'static str {
        match self {
            Self::PrefixBirthDateMismatch => "PREFIX_BIRTH_DATE_MISMATCH",
            Self::PrefixBirthPlaceMismatch => "PREFIX_BIRTH_PLACE_MISMATCH",
            Self::PrefixFirstRegistrationMismatch => "PREFIX_FIRST_REGISTRATION_MISMATCH",
        }
    }
}

impl HkidCard {
    /// Checks the supplied dates and reported birthplace against the available prefix metadata.
    /// Each applicable reason appears once, in declaration order.
    pub fn find_suspicions(&self) -> Vec<HkidSuspicion> {
        let fields = self.fields();
        let Some(prefix) = fields
            .hkid_number
            .as_ref()
            .and_then(HkidNumber::defined_prefix)
        else {
            return Vec::new();
        };
        let mut reasons = Vec::new();
        if let Some((_, end)) = prefix.birth_registration_period()
            && !fields.symbols.contains(HkidSymbol::BirthDetailsChanged)
        {
            if let (Some(birth), Some(end)) = (fields.date_of_birth, end)
                && birth >= end
            {
                reasons.push(HkidSuspicion::PrefixBirthDateMismatch);
            }
            if fields
                .symbols
                .by_category(HkidSymbolCategory::ReportedPlaceOfBirth)
                .any(|place| place != HkidSymbol::BornInHongKong)
            {
                reasons.push(HkidSuspicion::PrefixBirthPlaceMismatch);
            }
        }
        // The published first-issue period for L omits its earlier use as a reserve prefix.
        if let Some(month) = fields.first_registration_year_month
            && prefix.id() != "L"
            && prefix.first_issue_period().is_some()
            && !prefix.supports_first_issue_month(month.year(), month.month(), None)
        {
            reasons.push(HkidSuspicion::PrefixFirstRegistrationMismatch);
        }
        reasons
    }

    /// Whether any of the advisory prefix checks found a mismatch.
    pub fn is_suspicious(&self) -> bool {
        !self.find_suspicions().is_empty()
    }

    /// Describes the holder using today's date in the system time zone.
    /// A missing language tag selects the system locale at the time of the call.
    pub fn describe_holder(&self, locale: Option<&str>) -> Result<String, CardError> {
        self.describe_holder_on(locale, local_today())
    }

    /// Summarizes the holder's age on the supplied date, sex, reported birthplace
    /// and printed symbols. The summary omits identifying fields.
    ///
    /// Supply a BCP 47 tag such as `zh-HK` or `en`, or `None` for the current
    /// system locale. Tags whose language is `zh` select Traditional Chinese.
    /// Other languages and an unavailable system locale select English.
    ///
    /// Returns [`CardError::UnsupportedHolderSymbol`] if the symbol table assigns a symbol
    /// on this card to a birthplace, residential status or re-entry permit category
    /// that the symbol cannot describe.
    pub fn describe_holder_on(
        &self,
        locale: Option<&str>,
        reference_date: NaiveDate,
    ) -> Result<String, CardError> {
        let age = self.age_on(reference_date)?;
        let language = locale.map(Language::from_tag).unwrap_or_else(|| {
            system_locale()
                .as_deref()
                .map_or(Language::English, Language::from_tag)
        });
        let fields = self.fields();
        let symbols = &fields.symbols;
        let in_category = |category| symbols.by_category(category).next();
        let mut sections = Vec::new();
        if let Some(details) = language.demographics(
            age,
            fields.sex,
            in_category(HkidSymbolCategory::ReportedPlaceOfBirth),
        )? {
            sections.push(details);
        }
        if let Some(status) = language.status_and_eligibility(
            in_category(HkidSymbolCategory::ResidentialStatus),
            in_category(HkidSymbolCategory::ReEntryPermitEligibility),
        )? {
            sections.push(status);
        }
        if let Some(amendments) = language.amendments(symbols) {
            sections.push(amendments.to_owned());
        }
        Ok(sections.join(language.text(" ", "")))
    }
}

#[cfg(all(target_arch = "wasm32", target_os = "unknown"))]
fn system_locale() -> Option<String> {
    // Window and worker navigators both expose the preferred BCP 47 language.
    // Other JavaScript hosts may have no navigator.
    let navigator = js_sys::Reflect::get(&js_sys::global(), &"navigator".into()).ok()?;
    js_sys::Reflect::get(&navigator, &"language".into())
        .ok()?
        .as_string()
}

pub(crate) fn local_today() -> NaiveDate {
    #[cfg(all(target_os = "linux", target_env = "gnu"))]
    if let Some(date) = glibc_local_date() {
        return date;
    }
    #[cfg(all(unix, not(all(target_os = "linux", target_env = "gnu"))))]
    {
        // Chrono caches the Unix time zone briefly. Read TZ or /etc/localtime afresh
        // so consecutive calls can observe a change across the international date line.
        let now = chrono::Utc::now();
        let zone = match std::env::var("TZ") {
            Ok(value) if value.is_empty() => return now.date_naive(),
            Ok(value) => tz::TimeZone::from_posix_tz(&value),
            Err(_) => tz::TimeZone::local(),
        };
        if let Ok(zone) = zone
            && let Ok(local_time) = zone.find_local_time_type(now.timestamp())
        {
            let offset = chrono::TimeDelta::seconds(i64::from(local_time.ut_offset()));
            return (now + offset).date_naive();
        }
    }
    Local::now().date_naive()
}

#[cfg(all(target_os = "linux", target_env = "gnu"))]
fn glibc_local_date() -> Option<NaiveDate> {
    unsafe extern "C" {
        fn tzset();
    }
    let mut local = std::mem::MaybeUninit::<libc::tm>::uninit();
    // SAFETY: tzset and localtime_r synchronize glibc's time zone state.
    // tzset refreshes TZ before localtime_r, which writes to our own tm and
    // initializes it on success. time accepts null for its optional output.
    let local = unsafe {
        tzset();
        let now = libc::time(std::ptr::null_mut());
        if now == -1 || libc::localtime_r(&now, local.as_mut_ptr()).is_null() {
            return None;
        }
        local.assume_init()
    };
    NaiveDate::from_ymd_opt(
        local.tm_year.checked_add(1900)?,
        (local.tm_mon + 1) as u32,
        local.tm_mday as u32,
    )
}

#[derive(Clone, Copy)]
enum Language {
    English,
    TraditionalChinese,
}

impl Language {
    fn from_tag(tag: &str) -> Self {
        let mut subtags = tag.split('-');
        if !subtags
            .next()
            .is_some_and(|lang| lang.eq_ignore_ascii_case("zh"))
        {
            return Self::English;
        }
        // An extended language such as yue in zh-yue-HK is itself the language.
        // Three grandfathered tags spell their specific language with a longer word.
        let extended_language = subtags.next().is_some_and(|part| {
            part.len() == 3 && part.bytes().all(|byte| byte.is_ascii_alphabetic())
        });
        let grandfathered = ["zh-guoyu", "zh-hakka", "zh-xiang"]
            .iter()
            .any(|alias| tag.eq_ignore_ascii_case(alias));
        if extended_language || grandfathered {
            Self::English
        } else {
            Self::TraditionalChinese
        }
    }

    fn text(self, english: &'static str, chinese: &'static str) -> &'static str {
        match self {
            Self::English => english,
            Self::TraditionalChinese => chinese,
        }
    }

    fn demographics(
        self,
        age: Option<i32>,
        sex: Option<Sex>,
        birthplace: Option<HkidSymbol>,
    ) -> Result<Option<String>, CardError> {
        let sex = sex.map(|value| match value {
            Sex::Male => self.text("male", "男性"),
            Sex::Female => self.text("female", "女性"),
        });
        let demographics = match (self, sex, age) {
            (Self::English, Some(sex), Some(age)) => format!("is {sex}, aged {age}"),
            (Self::English, Some(sex), None) => format!("is {sex}"),
            (Self::English, None, Some(1)) => "is 1 year old".to_owned(),
            (Self::English, None, Some(age)) => format!("is {age} years old"),
            (Self::TraditionalChinese, Some(sex), Some(age)) => format!("為{age}歲{sex}"),
            (Self::TraditionalChinese, Some(sex), None) => format!("為{sex}"),
            (Self::TraditionalChinese, None, Some(age)) => format!("現年{age}歲"),
            (_, None, None) => String::new(),
        };
        let birthplace = match birthplace {
            Some(HkidSymbol::BornInHongKong) => self.text("was born in Hong Kong", "於香港出生"),
            Some(HkidSymbol::BornInMainland) => {
                self.text("was born in the Chinese Mainland", "於內地出生")
            }
            Some(HkidSymbol::BornInMacao) => self.text("was born in Macao", "於澳門出生"),
            Some(HkidSymbol::BornElsewhere) => self.text("was born elsewhere", "於其他地區出生"),
            None => "",
            Some(symbol) => {
                return Err(CardError::UnsupportedHolderSymbol {
                    symbol,
                    category: HkidSymbolCategory::ReportedPlaceOfBirth,
                });
            }
        };
        if demographics.is_empty() && birthplace.is_empty() {
            return Ok(None);
        }
        let separator = if demographics.is_empty() || birthplace.is_empty() {
            ""
        } else if sex.is_some() && age.is_some() {
            self.text(", and ", "，")
        } else {
            self.text(" and ", "，")
        };
        let subject = self.text("The holder ", "持有人");
        let stop = self.text(".", "。");
        Ok(Some(format!(
            "{subject}{demographics}{separator}{birthplace}{stop}"
        )))
    }

    fn status_and_eligibility(
        self,
        status: Option<HkidSymbol>,
        eligibility: Option<HkidSymbol>,
    ) -> Result<Option<String>, CardError> {
        let status_clause = match status {
            Some(HkidSymbol::RightOfAbode) => {
                self.text("has the right of abode in Hong Kong", "享有香港居留權")
            }
            Some(HkidSymbol::RightToLand) => {
                self.text("has the right to land in Hong Kong", "享有香港入境權")
            }
            Some(HkidSymbol::StayLimited) => self.text("had a stay restriction", "有逗留限制"),
            Some(HkidSymbol::StayNotLimited) => {
                self.text("had no stay restriction", "沒有逗留限制")
            }
            None => "",
            Some(symbol) => {
                return Err(CardError::UnsupportedHolderSymbol {
                    symbol,
                    category: HkidSymbolCategory::ResidentialStatus,
                });
            }
        };
        let eligibility = match eligibility {
            Some(HkidSymbol::AdultReEntryPermit) => self.text(
                "was aged 18 or over and eligible for a Re-entry Permit",
                "年滿18歲，並符合回港證申領資格",
            ),
            Some(HkidSymbol::MinorReEntryPermit) => self.text(
                "was aged 11 to 17 and eligible for a Re-entry Permit",
                "年齡為11至17歲，並符合回港證申領資格",
            ),
            None => "",
            Some(symbol) => {
                return Err(CardError::UnsupportedHolderSymbol {
                    symbol,
                    category: HkidSymbolCategory::ReEntryPermitEligibility,
                });
            }
        };
        if status_clause.is_empty() && eligibility.is_empty() {
            return Ok(None);
        }
        let present_tense = matches!(
            status,
            Some(HkidSymbol::RightOfAbode | HkidSymbol::RightToLand)
        );
        let subject = if present_tense {
            self.text("The holder ", "持有人")
        } else {
            self.text("At registration, the holder ", "持有人於領證登記時")
        };
        let separator = if status_clause.is_empty() || eligibility.is_empty() {
            ""
        } else if present_tense {
            self.text(" and, at registration, ", "，於領證登記時")
        } else {
            self.text(" and ", "，")
        };
        let stop = self.text(".", "。");
        Ok(Some(format!(
            "{subject}{status_clause}{separator}{eligibility}{stop}"
        )))
    }

    fn amendments(self, symbols: &HkidSymbols) -> Option<&'static str> {
        match (
            symbols.contains(HkidSymbol::BirthDetailsChanged),
            symbols.contains(HkidSymbol::NameChanged),
        ) {
            (true, true) => Some(self.text(
                "The holder's date or place of birth has been amended, as has their name.",
                "持有人的出生日期或出生地曾更改，姓名亦曾更改。",
            )),
            (true, false) => Some(self.text(
                "The holder's date or place of birth has been amended.",
                "持有人的出生日期或出生地曾更改。",
            )),
            (false, true) => Some(self.text(
                "The holder's name has been amended.",
                "持有人的姓名曾更改。",
            )),
            (false, false) => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::card::HkidCardFields;

    #[test]
    fn explicit_locale_selects_the_language_subtag_case_insensitively() {
        let card = HkidCard::new(HkidCardFields {
            sex: Some(Sex::Male),
            ..Default::default()
        })
        .unwrap();
        let date = "2026-07-21".parse().unwrap();
        for (tag, expected) in [
            ("ZH-hans-CN", "持有人為男性。"),
            ("zh-419", "持有人為男性。"),
            ("zh-yue-HK", "The holder is male."),
            ("zh-cmn-Hans", "The holder is male."),
            ("ZH-HAKKA", "The holder is male."),
            ("zhx", "The holder is male."),
            ("en-u-rg-cnzzzz", "The holder is male."),
        ] {
            assert_eq!(
                card.describe_holder_on(Some(tag), date).unwrap(),
                expected,
                "{tag}"
            );
        }
    }

    #[test]
    fn description_reports_a_reference_date_before_birth() {
        let card = HkidCard::new(HkidCardFields {
            date_of_birth: Some("2026-07-22".parse().unwrap()),
            ..Default::default()
        })
        .unwrap();
        assert!(
            card.describe_holder_on(Some("en"), "2026-07-21".parse().unwrap())
                .is_err()
        );
    }

    #[test]
    fn misclassified_birthplace_is_reported_with_or_without_other_demographics() {
        let symbol = HkidSymbol::RightOfAbode;
        for language in [Language::English, Language::TraditionalChinese] {
            for (age, sex) in [(None, None), (Some(24), Some(Sex::Female))] {
                assert_eq!(
                    language.demographics(age, sex, Some(symbol)),
                    Err(CardError::UnsupportedHolderSymbol {
                        symbol,
                        category: HkidSymbolCategory::ReportedPlaceOfBirth,
                    })
                );
            }
        }
    }

    #[test]
    fn misclassified_status_is_reported_before_eligibility() {
        let symbol = HkidSymbol::BornInHongKong;
        for eligibility in [None, Some(HkidSymbol::NameChanged)] {
            assert_eq!(
                Language::English.status_and_eligibility(Some(symbol), eligibility),
                Err(CardError::UnsupportedHolderSymbol {
                    symbol,
                    category: HkidSymbolCategory::ResidentialStatus,
                })
            );
        }
    }

    #[test]
    fn misclassified_eligibility_is_reported_with_or_without_status() {
        let symbol = HkidSymbol::NameChanged;
        for status in [None, Some(HkidSymbol::RightOfAbode)] {
            assert_eq!(
                Language::English.status_and_eligibility(status, Some(symbol)),
                Err(CardError::UnsupportedHolderSymbol {
                    symbol,
                    category: HkidSymbolCategory::ReEntryPermitEligibility,
                })
            );
        }
    }
}
