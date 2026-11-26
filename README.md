# HKID Utilities 香港身份證工具庫

Utilities for Hong Kong Identity Card (HKID) validation, check-digit
calculation, formatting and test data generation. Handles Hong Kong-specific 
rules and edge cases so you don't have to implement them from scratch.

- **Maintained Hong Kong reference data ｜ 香港身份證字頭持續更新**

  Includes the [27 July 2026 F-to-L prefix transition](https://accessinfo.hk/request/guan_yu_xiang_gang_shen_fen_zhen),
  with date-aware prefix selection and boundary tests.

- **Hong Kong age rules ｜ 香港法定歲數計算方式**

  For 29 February births, the age anniversary is 1 March in non-leap years,
  following [Cap. 410, section 5](https://www.elegislation.gov.hk/hk/cap410!en/s5).
  Card-symbol checks use the holder's age at registration.

- **Consistent test data ｜ 欄位一致嘅生成資料**

  Generated cards align Chinese names, Chinese Commercial Codes (中文電碼)
  and Cantonese romanisation (粵語拼音), with compatible prefixes, dates and
  age-specific symbols.

## Why this project?

(Cantonese) 喺香港3間公司、3個 system 入面寫過3次相同嘅 code 之後，我決定整一個可以重用、持續維護嘅 library，幫大家慳返查資料、處理 edge cases 嘅時間，同埋 token。

## Quick start

### CPython 3.11+

Install the [Python package](python/README-Python.md) from PyPI and validate an HKID number.

```shell
python -m pip install --only-binary=:all: hkid-utils
```

```python
from hkid_utils import is_valid

is_valid("A123456(3)")  # True
```

### Java 8+

Add `hkid-utils` to the `<dependencies>` section of your Maven `pom.xml`.

```xml
<dependency>
    <groupId>io.github.wal-n</groupId>
    <artifactId>hkid-utils</artifactId>
    <version>1.2.0</version>
</dependency>
```

#### Example

```java
import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.HkidCardUtil;
import io.github.wal_n.hkid.card.HkidSymbolsUtil;
import io.github.wal_n.hkid.name.ChineseNameUtil;
import io.github.wal_n.hkid.name.EnglishNameUtil;
import io.github.wal_n.hkid.number.HkidNumber;
import io.github.wal_n.hkid.number.HkidNumberUtil;

import java.time.LocalDate;
import java.util.Locale;

public final class HkidExample {
  public static void main(String[] args) {
    // Validation
    HkidNumberUtil.isValid("A123456(3)");       // true
    HkidNumberUtil.isValid("A123456(4)");       // false (expected check digit 3)
    ChineseNameUtil.isValid("陳", "大文");       // true
    EnglishNameUtil.isValid("Chan", "Tai Man"); // true
    HkidSymbolsUtil.isValid("***AZ");           // true

    // Formatting
    HkidNumber number = new HkidNumber("A123456");
    System.out.println(number);                  // A123456(3)
    System.out.println(number.toMaskedString()); // ****456(*)

    // Cross-field checks
    // The Z prefix was used for Hong Kong birth registrations from 1980 to 1988.
    HkidCard cardToCheck = HkidCard.builder()
      .hkidNumber(new HkidNumber("Z123456"))
      .dateOfBirth(LocalDate.of(2000, 1, 1))
      .symbolCodes("X") // Reported birthplace is mainland China.
      .build();
    cardToCheck.isSuspicious(); // true
    System.out.println(cardToCheck.findSuspicions());
    // [PREFIX_BIRTH_DATE_MISMATCH, PREFIX_BIRTH_PLACE_MISMATCH]

    // Generate data. The output below is one example and varies with each run.
    LocalDate referenceDate = LocalDate.of(2026, 7, 21);
    HkidCard sample = HkidCardUtil.generateRandomCard(referenceDate);
    System.out.println(sample.getChineseName());            // 李妍
    System.out.println(sample.getChineseCommercialCodes()); // [2621, 1177]
    System.out.println(sample.getEnglishName());            // Lee, Yin
    System.out.println(sample.getSex());                    // 女 F
    System.out.println(sample.getHkidNumber());             // K977182(4)
    System.out.println(sample.getSymbols());                // ***AZ

    // Holder summaries
    System.out.println(sample.describeHolder(Locale.TRADITIONAL_CHINESE));
    // 持有人為91歲女性，於香港出生。她享有香港居留權，於領證登記時年滿18歲，並符合回港證申領資格。
    System.out.println(sample.describeHolder(Locale.ENGLISH));
    // The holder is female, aged 91, and was born in Hong Kong. She has the
    // right of abode in Hong Kong and, at registration, was aged 18 or over
    // and eligible for a Re-entry Permit.
  }
}
```

### Rust 1.100+

Add the crate to your project's `Cargo.toml`.

```toml
[dependencies]
hkid-utils = "1.2.0"
```

For a local checkout, use `hkid-utils = { path = "../hkid" }` in the same section.

Save this as `src/main.rs` and run `cargo run`.

```rust
use hkid_utils::{
    ChineseName, EnglishName, HkidCard, HkidCardFields, HkidGenerator, HkidNumber, HkidSymbols,
};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Validation
    println!("{}", HkidNumber::is_valid("A123456(3)")); // true
    println!("{}", HkidNumber::is_valid("A123456(4)")); // false (expected check digit 3)
    println!("{}", ChineseName::is_valid(Some("陳"), Some("大文"))); // true
    println!("{}", EnglishName::is_valid(Some("Chan"), Some("Tai Man"))); // true
    println!("{}", HkidSymbols::is_valid("***AZ")); // true

    // Formatting
    let number: HkidNumber = "A123456".parse()?;
    println!("{number}"); // A123456(3)
    println!("{}", number.masked()); // ****456(*)

    // Cross-field checks
    let card = HkidCard::new(HkidCardFields {
        hkid_number: Some("Z123456".parse()?),
        date_of_birth: Some("2000-01-01".parse()?),
        symbols: "X".parse()?,
        ..Default::default()
    })?;
    println!("{:?}", card.find_suspicions()); // [PrefixBirthDateMismatch, PrefixBirthPlaceMismatch]

    // Generate data. The output below is from one run.
    let reference = "2026-07-21".parse()?;
    let sample = HkidGenerator::new().card_on(reference, None)?;
    let fields = sample.fields();
    println!("{}", fields.chinese_name); // 何國德
    for code in fields.chinese_name.commercial_codes() {
        println!("{code}"); // 0149, 0948 and 1795
    }
    println!("{}", fields.english_name); // Ho, Kwok Tak
    // Generated cards always have a sex and an HKID number.
    println!("{}", fields.sex.unwrap()); // 男 M
    println!("{}", fields.hkid_number.as_ref().unwrap()); // Z557054(A)
    println!("{}", fields.symbols); // ***AZ

    // Holder summaries
    println!("{}", sample.describe_holder(Some("zh-HK"))?);
    // 持有人為39歲男性，於香港出生。他享有香港居留權，於領證登記時年滿18歲，並符合回港證申領資格。
    println!("{}", sample.describe_holder(Some("en"))?);
    // The holder is male, aged 39, and was born in Hong Kong. He has the
    // right of abode in Hong Kong and, at registration, was aged 18 or over
    // and eligible for a Re-entry Permit.
    Ok(())
}
```

More examples are in the [Rust crate documentation](rust/README.md).

> [!CAUTION]
> Generated HKID numbers may coincide with real people's numbers. Use them only
> in isolated test datasets and environments. A valid checksum does not guarantee
> a fictional identity.

## Usage notes

### Validation and formatting

An HKID number has a one- or two-letter prefix followed by six digits.
You can omit the check digit, append it directly or enclose it in matching
ASCII `()` or fullwidth `（）` parentheses. `HkidNumber` calculates a missing
check digit and formats it with ASCII parentheses. If the format or supplied
check digit is invalid, `HkidNumberUtil.isValid` returns `false`.

Number and symbol text parsers convert fullwidth Latin letters and digits to
ASCII, accepting mixtures such as `Ａ12３456(３)` and formatting numbers as
`A123456(3)`. Other Unicode letters, digits and punctuation are not converted.

Number and symbol text inputs accept specified outer whitespace and spaces
between complete components, such as `A 123456 (3)` and `*** A Z`. See the
[shared parsing rules](conformance/README.md#number-and-symbol-text-parsing)
for the exact character sets. Constructors taking separate number components
require ASCII input and do not trim or normalize character width.

Building a card checks date order and age-specific symbols.
Call `validateAsOf(referenceDate)` to reject future-dated fields.
`findSuspicions()` lists inconsistencies between the available fields.

### Generated data

Call `HkidCardUtil.generateRandomCard()` in Java or `HkidGenerator::new().card(None)`
in Rust for a sample as of today. 
The name generator uses sex-associated and unisex entries. Name validation
accepts names independently of those associations.

### Holder summaries

`describeHolder()` reports age on the reference date,
sex, reported birthplace and symbol meanings when those fields are available.
Age-specific and stay-restriction symbols describe the holder at registration.
The summary omits names, Chinese Commercial Codes, exact birth dates, HKID
numbers and registration dates. It returns an empty string for a card with
nothing to describe.

## Disclaimer

Independent and unofficial; not affiliated with or endorsed by the Hong Kong
SAR Government. Checks data formats and consistency, not identity or card
authenticity. Do not use as an authoritative source for identity verification,
legal decisions or other decisions affecting a person.

The bundled prefix metadata, symbol descriptions and name seeds are
non-authoritative and may be incomplete, outdated or wrong. To customise them,
edit `data/` and rebuild.

## License

[MIT](LICENSE)
