# HKID Utilities 香港身份證工具庫

Utilities for working with Hong Kong Identity Card (HKID) data. The Java
library can:

- validate HKID numbers
  驗證香港身份證號碼
- validate Chinese and English names and current smart-HKID symbols
  驗證中英文姓名同埋智能身份證符號
- model complete cards and generate internally consistent sample data
  生成欄位資訊一致嘅完整身份證資料

## Java

### Requirements

- Java 8+

### Maven dependency

Add the following dependency inside the `<dependencies>` section of your
`pom.xml`:

```xml
<dependency>
    <groupId>io.github.wal-n</groupId>
    <artifactId>hkid-utils</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Usage

#### Validation and formatting

```java
import io.github.wal_n.hkid.card.HkidSymbolsUtil;
import io.github.wal_n.hkid.name.ChineseNameUtil;
import io.github.wal_n.hkid.name.EnglishNameUtil;
import io.github.wal_n.hkid.number.HkidNumber;
import io.github.wal_n.hkid.number.HkidNumberUtil;

public final class ValidationExample {
    public static void main(String[] args) {
        HkidNumber number = new HkidNumber("A123456");

        System.out.println(number.toString(HkidNumber.Format.COMPLETE)); // A123456(3)
        System.out.println(number.toMaskedString());                     // ****456(*)

        System.out.println(HkidNumberUtil.isValid("A123456(3)"));
        System.out.println(ChineseNameUtil.isValid("陳", "大文"));
        System.out.println(EnglishNameUtil.isValid("Chan", "Tai Man"));
        System.out.println(HkidSymbolsUtil.isValid("***AZ"));
    }
}
```

HKID inputs may use a one- or two-letter prefix and may omit the check digit.
When supplied, a bare or parenthesised check digit is validated automatically.
Use the `isValid` helpers when invalid input should return `false` instead of
throwing an exception.

Card construction additionally validates date order and age-specific symbols.
Call `validateAsOf(referenceDate)` to reject future-dated fields.

#### Generated data

```java
import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.HkidCardUtil;

public final class GeneratedDataExample {
    public static void main(String[] args) {
        HkidCard card = HkidCardUtil.generateRandomCard();

        System.out.println(card.getChineseName());              // e.g. 陳大文
        System.out.println(card.getChineseCommercialCodes());   // e.g. [7115, 1129, 2429]
        System.out.println(card.getEnglishName());              // e.g. Chan, Tai Man
        System.out.println(card.getSex());                      // e.g. 男 M
        System.out.println(card.getHkidNumber());               // e.g. A123456
        System.out.println(card.getSymbols());                  // e.g. ***AZBN
    }
}
```

> [!CAUTION]
> A randomly generated, mathematically valid HKID number can still coincide
> with a number assigned to a real person, however unlikely. Use generated
> data only in isolated test datasets and environments, and do not treat it
> as a guarantee of a fictional or non-existent identity. Never mix generated
> identity data with production or customer data.

The generator keeps Chinese characters, Chinese Commercial Codes, and
Cantonese romanisation aligned. Sex-specific generation includes unisex seeds
alongside those associated with the selected sex. These associations are
generation hints, not strict properties of real names.

## Shared data and conformance

Language-neutral tables are documented in [`data/README.md`](data/README.md).
Cross-language behaviour fixtures are documented in
[`conformance/README.md`](conformance/README.md).

The bundled prefix metadata, smart-HKID symbol descriptions, and name seeds are
non-authoritative starter data. They may be incomplete, outdated, or wrong. If
they do not suit your use case, update the files under `data/` and rebuild the
library with your own values.

## Project structure

- `java/` — Java 8+ implementation and Maven build
- `data/` — language-neutral reference data
- `conformance/` — deterministic behaviour fixtures shared by implementations

The Java API is split across `number`, `name`, and `card` packages under
`io.github.wal_n.hkid`. Models are immutable: build a card with
`HkidCard.builder()`, or copy one with `toBuilder()`.

## Disclaimer

This is an independent, unofficial project and is not affiliated with or
endorsed by the Hong Kong SAR Government or any of its departments.

It validates data formats and consistency rules; it does not prove that a card
or person is genuine. Do not use it as an authoritative source for identity
verification, legal decisions, or other decisions affecting a person.

## License

[MIT](LICENSE)
