# HKID Utilities 香港身份證工具庫

Utilities for Hong Kong Identity Card (HKID) validation, check-digit
calculation, formatting and test data generation. Handles Hong Kong-specific 
rules and edge cases so you don't have to implement them from scratch.

- **Maintained Hong Kong reference data ｜ 香港身份證字頭持續更新**

  Includes the [27 July 2026 F-to-L prefix transition](https://accessinfo.hk/request/guan_yu_xiang_gang_shen_fen_zhen),
  with date-aware prefix selection and boundary tests.

- **Hong Kong age rules ｜ 香港歲數計算、閏日生日**

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

Currently available for Java 8+. Support for more languages is planned.

Add to your `pom.xml` for Maven dependencies:

```xml
<dependency>
    <groupId>io.github.wal-n</groupId>
    <artifactId>hkid-utils</artifactId>
    <version>0.2.0</version>
</dependency>
```

### Validation and formatting

```java
import io.github.wal_n.hkid.card.HkidSymbolsUtil;
import io.github.wal_n.hkid.name.ChineseNameUtil;
import io.github.wal_n.hkid.name.EnglishNameUtil;
import io.github.wal_n.hkid.number.HkidNumber;
import io.github.wal_n.hkid.number.HkidNumberUtil;

public final class ValidationExample {
  public static void main(String[] args) {
    HkidNumber number = new HkidNumber("A123456");

    System.out.println(number);                  // A123456(3)
    System.out.println(number.toMaskedString()); // ****456(*)

    System.out.println(HkidNumberUtil.isValid("A123456(3)"));
    System.out.println(ChineseNameUtil.isValid("陳", "大文"));
    System.out.println(EnglishNameUtil.isValid("Chan", "Tai Man"));
    System.out.println(HkidSymbolsUtil.isValid("***AZ"));
  }
}
```

Accepts one- or two-letter prefixes and an optional bare or parenthesised
check digit. Supplied check digits are validated; `isValid` returns `false`
for invalid input.

Card construction additionally validates date order and age-specific symbols.
Call `validateAsOf(referenceDate)` to reject future-dated fields.

### Generated data

```java
import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.HkidCardUtil;

public final class GeneratedDataExample {
  public static void main(String[] args) {
    HkidCard card = HkidCardUtil.generateRandomCard();

    System.out.println(card.getChineseName());              // e.g. 樂永晴
    System.out.println(card.getChineseCommercialCodes());   // e.g. [2867, 3057, 2532]
    System.out.println(card.getEnglishName());              // e.g. LOK, Wing Ching
    System.out.println(card.getSex());                      // e.g. 女 F
    System.out.println(card.getHkidNumber());               // e.g. Z683365(A)
    System.out.println(card.getSymbols());                  // e.g. ***AZ
  }
}
```

> [!CAUTION]
> Generated HKID numbers may coincide with real people's numbers. Use only in
> isolated test datasets and environments; never mix with production or customer
> data. A valid checksum does not guarantee a fictional identity.

Name seeds include sex-associated and unisex entries; these guide generation
and do not restrict validation of real names.

## For AI agents

Use the [Java library](java/) and Maven dependency above for validation and
generation. Consult the [reference data](data/README.md) and
[JSON conformance fixtures](conformance/README.md) for input examples and
expected behaviour.

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
