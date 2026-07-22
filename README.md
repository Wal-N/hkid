# HKID Utilities

Java utilities and models for working with Hong Kong Identity Card (HKID) data.
The project models both an HKID number and the wider data printed on an HKID
card, including names, sex marker, card symbols, and registration dates.

## Current API

- `hkid.HkidNum` represents an HKID number with a one- or two-letter prefix,
  six numerals, and a calculated check digit.
- `hkid.HkidNumUtil` generates random HKID numbers.
- `hkid.HkidCard` represents the data printed on an HKID card.
- `hkid.HkidCardUtil` generates sample `HkidCard` objects and random dates.
- `hkid.HkidNameUtil` generates Chinese names with matching commercial codes
  and Hong Kong Government Cantonese romanised English names.
- `hkid.GeneratedName` contains the generated `ChiName`, `EngName`,
  commercial codes, and romanisation syllables.
- `hkid.ChiName` and `hkid.ChiNameUtil` validate Chinese names and four-digit
  commercial codes.
- `hkid.EngName` and `hkid.EngNameUtil` validate English names.
- `hkid.Sex` represents the Chinese and English sex markers printed on the
  smart HKID, forming the complete value `男 M` or `女 F`.
- `hkid.HkidSymbol` defines the symbols supported by the current smart HKID.
- `hkid.HkidSymbols` parses, orders, and validates complete symbol strings such
  as `***AZ`.

## Requirements

- Java 8 or higher
- Maven 3.8 or higher for the normal build and test workflow

## Build And Test

Run the full JUnit 5 test suite:

```powershell
mvn test
```

In IntelliJ IDEA, import `pom.xml` as a Maven project so that
`src/main/resources` is included on the runtime classpath. If the project is
opened as a plain Java module, mark `src/main/resources` as **Resources Root**.

Build the jar without rerunning tests:

```powershell
mvn -DskipTests package
```

If Maven is not installed, you can still compile the main classes from
PowerShell:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

`HkidCardTest` is a JUnit integration test under `src/test/java` and is not included
in the published jar.

## HKID Number Usage

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        HkidNum hkidNum = new HkidNum("A123456(3)");

        System.out.println(hkidNum.getPrefix());       // A
        System.out.println(hkidNum.getNumerals());     // 123456
        System.out.println(hkidNum.getCheckDigit());   // 3
    }
}
```

Accepted input formats include:

- `A123456`
- `A1234563`
- `A123456(3)`
- `AB123456`
- `AB1234569`
- `AB123456(9)`

If an input check digit is present, it is validated during construction:

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        try {
            new HkidNum("A123456(7)");
        } catch (HkidNum.InvalidCheckDigitException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

You can also validate a check digit explicitly when the HKID number is supplied
without the check digit:

```java
boolean valid = HkidNum.validateCheckDigit("A123456", "3");
```

## Formatting

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        HkidNum hkidNum = new HkidNum("A123456");

        System.out.println(hkidNum.toString());                                   // A123456
        System.out.println(hkidNum.toString(HkidNum.Format.WithoutCheckDigit));   // A123456
        System.out.println(hkidNum.toString(HkidNum.Format.WithoutParentheses));  // A1234563
        System.out.println(hkidNum.toString(HkidNum.Format.Complete));            // A123456(3)
    }
}
```

## Random HKID Numbers

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        HkidNum definedPrefix = HkidNumUtil.genRandomHkidNum();
        HkidNum anyPrefix = HkidNumUtil.genRandomHkidNum(false);

        System.out.println(definedPrefix.toString(HkidNum.Format.Complete));
        System.out.println(anyPrefix.toString(HkidNum.Format.Complete));
    }
}
```

By default, random numbers use one of the predefined prefixes in
`HkidNum.DefinedPrefix`. Pass `false` to allow any one- or two-letter prefix.

## Prefix Descriptions

`getPrefixDescription()` and `getPrefixTraditionalChineseDescription()` are safe
for every valid one- or two-letter prefix. They return a fallback message when a
prefix has no predefined metadata. Use `getDefinedPrefix()` when callers need to
distinguish that case explicitly:

```java
HkidNum hkidNum = new HkidNum("Q123456");

String english = hkidNum.getPrefixDescription();
String traditionalChinese = hkidNum.getPrefixTraditionalChineseDescription();
Optional<HkidNum.DefinedPrefix> metadata = hkidNum.getDefinedPrefix();
```

The enum also exposes `getDescription()`,
`getTraditionalChineseDescription()`, and the case-insensitive
`DefinedPrefix.fromPrefix(String)` lookup.

## Random Names

`HkidNameUtil` generates a Chinese name, matching Chinese Commercial Codes, and
an English name using Hong Kong Government Cantonese Romanisation.
The no-argument `genRandomName()` has a 10% chance of generating a one-character
personal name and a 90% chance of generating a two-character personal name.

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        GeneratedName oneCharName = HkidNameUtil.genRandomName(1);
        GeneratedName twoCharName = HkidNameUtil.genRandomName(2);

        System.out.println(oneCharName.getChiFullName());
        System.out.println(oneCharName.getCommercialCodes());
        System.out.println(oneCharName.getRomanisation());
        System.out.println(oneCharName.getEngFullName());

        System.out.println(twoCharName.getChiFullName());
        System.out.println(twoCharName.getCommercialCodes());
        System.out.println(twoCharName.getRomanisation());
        System.out.println(twoCharName.getEngFullName());
    }
}
```

Seed data lives in `src/main/resources/hkid/chinese-name-seed.csv`. It is a
small starter database, not a complete Chinese name database. The columns are:

```text
commercialCode,character,romanisation,commonSurname,weight
```

Rows with `commonSurname=true` are used as surname seeds; rows with
`commonSurname=false` are used as personal-name seeds. `weight` biases random
selection within each group. Each seed row stores one canonical romanisation
because Hong Kong Government Cantonese Romanisation can have multiple accepted
spellings for the same character.

## Complete HKID Card Data

Use `HkidCard` when you need to model more than the card number:

```java
package hkid;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

public class Example {
    public static void main(String[] args) {
        HkidCard card = new HkidCard();
        card.setHkidNum(new HkidNum("A123456(3)"));
        card.setChiName(new ChiName("陳", "大文",
                Arrays.asList("7115", "1129", "2429")));
        card.setEngName(new EngName("Chan", "Tai Man"));
        card.setSex(Sex.MALE);
        card.setDateOfBirth(LocalDate.of(1990, 1, 15));
        card.setSymbols(HkidSymbols.of(
                HkidSymbol.ADULT_RE_ENTRY_PERMIT,
                HkidSymbol.RIGHT_OF_ABODE,
                HkidSymbol.BORN_IN_HONG_KONG));
        card.setFirstRegistrationYearMonth(YearMonth.of(2001, 6));
        card.setDateOfRegistration(LocalDate.of(2020, 6, 1));

        System.out.println(card);
    }
}
```

You can also generate a sample card:

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        HkidCard card = HkidCardUtil.genRandomCard();

        System.out.println(card);
    }
}
```

`HkidCard.toString()` only includes the masked HKID number and date of
registration, for example:

```text
HkidCard[hkidNum=****456(*), dateOfRegistration=01-06-20]
```

Generated cards use a random `M` or `F` sex marker and an age from 11 to 100.
The current-card registration date is no earlier than 26 November 2018 and is
within the latest ten years. The month and year of first registration is between
the holder's 11th birthday and the current-card registration date. The generated
age symbol is `*` for ages 11-17 and `***` for ages 18 or above.

## Current Smart HKID Symbols

Only symbols used by the current smart HKID are supported. Parse a complete
symbol string directly from card or OCR input. Definitions follow the
[Immigration Department ROP133](https://www.immd.gov.hk/pdforms/rop133.pdf).

```java
HkidSymbols symbols = HkidSymbols.parse("***AZBN");

System.out.println(symbols); // ***AZBN
System.out.println(symbols.asList());
```

The parser accepts symbol letters without regard to case, returns symbols in
the official category order, and rejects duplicate, conflicting, unknown, or
legacy symbols. The re-entry permit, residential status, and reported place of
birth categories allow at most one symbol each. `B` and `N` may appear together.

An `HkidCard` can also read or set the complete printed value:

```java
HkidCard card = new HkidCard();
card.setDateOfBirth(LocalDate.of(1990, 1, 15));
card.setSymbolCodes("***AZ");

System.out.println(card.getSymbolCodes()); // ***AZ
```

## Validation Rules

- HKID number prefixes must be one or two uppercase letters.
- HKID number numerals must be exactly six digits.
- Check digits are calculated automatically and can be `0`-`9` or `A`.
- HKID number parsing rejects inputs that contain both plain and parenthesized
  check digits.
- Chinese name parts must contain Chinese characters only and have a combined
  maximum length of six characters. This includes supplementary-plane and HKSCS 
  ideographs, but not compatibility forms or radical symbols outside that property.
- Chinese commercial codes must be four digits each. When a Chinese name is set,
  the number of commercial codes must match the number of Chinese characters.
- Generated names keep the Chinese name, commercial codes, and romanisation
  syllables aligned character-by-character.
- English name parts must start with a letter and may contain letters, spaces,
  dots, apostrophes, or hyphens.
- English sex markers are parsed with `Sex.fromEngMarker("M")` or
  `Sex.fromEngMarker("F")`;
  `Sex.getPrintedValue()` returns the corresponding `男 M` or `女 F` card value.
- Current smart HKID symbols reject duplicate or conflicting categories. `*` or
  `***` must match the holder's age on the current-card registration date, so a
  historical juvenile card can still be represented after its holder turns 18.
- Use `validateAsOf(date)` to check the dated fields and age symbol at another
  point in time.
- The month and year of first registration cannot be before the holder's birth
  month or after the current-card registration date.
- Current smart HKID registration dates cannot be before 26 November 2018, and
  all card dates reject future or inconsistent values.
