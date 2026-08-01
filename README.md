# HKID Utilities

Utilities for parsing, validating, formatting, and generating Hong Kong
Identity Card (HKID) data. This repository contains shared reference data,
cross-language conformance fixtures, and the Java implementation.

The Java library includes:

- HKID numbers and check digits
- Chinese and English names
- current smart HKID symbols
- complete card models and sample-data generation

## Repository layout

- `java/` — Java 8+ implementation and Maven build
- `data/` — language-neutral reference data
- `conformance/` — deterministic behaviour fixtures shared by implementations

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

### Build from source

Run the Maven wrapper from the Java implementation directory:

```shell
cd java
bash ./mvnw verify
```

On Windows, use `.\mvnw.cmd verify` after changing to `java`.

### Quick start

#### HKID numbers

```java
import io.github.wal_n.hkid.number.HkidNumber;

HkidNumber number = new HkidNumber("A123456(3)");

System.out.println(number.getPrefix());       // A
System.out.println(number.getNumerals());     // 123456
System.out.println(number.getCheckDigit());   // 3
System.out.println(number.toMaskedString());  // ****456(*)

System.out.println(number.toString(
        HkidNumber.Format.COMPLETE));          // A123456(3)
```

Accepted forms include `A123456`, `A1234563`, `A123456(3)`, and their
two-letter-prefix equivalents. A supplied check digit is validated
automatically.

<sub>The bundled prefix table and its descriptions and date ranges are
non-authoritative and are best treated as internet folklore rather than
verified documentation. They may be incomplete, outdated, or wrong. If the
supplied data does not suit your use case, edit
<code>data/defined-prefixes.json</code> and rebuild the library with your own
values.</sub>

#### Validation

Validate HKID numbers, names, and current smart-HKID symbols without building
a complete card:

```java
import io.github.wal_n.hkid.card.HkidSymbolsUtil;
import io.github.wal_n.hkid.name.ChineseNameUtil;
import io.github.wal_n.hkid.name.EnglishNameUtil;
import io.github.wal_n.hkid.number.HkidNumberUtil;

boolean numberOk = HkidNumberUtil.isValid("A123456(3)");
boolean checkDigitOk = HkidNumberUtil.validateCheckDigit("A123456", "3");
boolean chineseOk = ChineseNameUtil.isValid("陳", "大文");
boolean englishOk = EnglishNameUtil.isValid("Chan", "Tai Man");
boolean symbolsOk = HkidSymbolsUtil.isValid("***AZ");
```

Card construction additionally validates date order and age-specific symbols.
Use `validateAsOf(referenceDate)` to reject future-dated fields.

#### Generated data

```java
import io.github.wal_n.hkid.card.HkidCard;
import io.github.wal_n.hkid.card.HkidCardUtil;
import io.github.wal_n.hkid.card.Sex;
import io.github.wal_n.hkid.name.GeneratedName;
import io.github.wal_n.hkid.name.HkidNameUtil;

HkidCard card = HkidCardUtil.generateRandomCard();
GeneratedName femaleName = HkidNameUtil.generateRandomName(Sex.FEMALE);

System.out.println(card.getChineseName());            // e.g. 陳大文
System.out.println(card.getChineseCommercialCodes()); // e.g. [7115, 1129, 2429]
System.out.println(card.getEnglishName());            // e.g. Chan, Tai Man
System.out.println(card.getSex());                    // e.g. 男 M
System.out.println(card.getHkidNumber());             // e.g. A123456
System.out.println(card.getSymbols());                // e.g. ***AZBN
System.out.println(femaleName.getChineseFullName());  // e.g. 李雅雯
```

> [!CAUTION]
> A randomly generated, mathematically valid HKID number can still coincide
> with a number assigned to a real person, however unlikely. Use generated
> data only in isolated test datasets and environments, and do not treat it
> as a guarantee of a fictional or non-existent identity. Never mix generated
> identity data with production or customer data.

The name generator keeps Chinese characters, Chinese Commercial Codes, and
Cantonese romanisation aligned. Given-name seed entries have a male, female, or
unisex association. Sex-specific generation includes unisex entries, and a
generated card selects its name from the pool matching its sex marker. These
associations are generation hints rather than strict properties of real names.
The seed file is a small, unverified starter dataset rather than an official or
complete name database.

### API layout

- `io.github.wal_n.hkid.number` — numbers, prefixes, and check digits
- `io.github.wal_n.hkid.name` — Chinese/English names and name generation
- `io.github.wal_n.hkid.card` — card models, sex markers, and smart-card symbols

Models are immutable. Build a card with `HkidCard.builder()`, or copy one with
`toBuilder()`.

## Shared data and conformance

Language-neutral tables are documented in [`data/README.md`](data/README.md).
Cross-language behaviour fixtures are documented in
[`conformance/README.md`](conformance/README.md).

The bundled symbol table and descriptions are non-authoritative and may be
incomplete, outdated, or wrong. If they do not suit your use case, edit
`data/hkid-symbols.json` and rebuild the library with your own values.

## Disclaimer

This is an independent, unofficial project and is not affiliated with or
endorsed by the Hong Kong SAR Government or any of its departments.

It validates data formats and consistency rules; it does not prove that a card
or person is genuine. Do not use it as an authoritative source for identity
verification, legal decisions, or other decisions affecting a person.

## License

[MIT](LICENSE)
