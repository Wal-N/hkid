# HKID Utilities

Java utilities and models for working with Hong Kong Identity Card (HKID) data.
The project models both an HKID number and the wider data printed on an HKID
card, including names, sex marker, card symbols, and registration dates.

## Current API

- `hkid.HkidNum` represents an HKID number with a one- or two-letter prefix,
  six numerals, and a calculated check digit.
- `hkid.HkidNumUtil` generates random HKID numbers.
- `hkid.HKID` represents the data printed on an HKID card.
- `hkid.HKIDUtil` generates sample `HKID` objects and random dates.
- `hkid.ChiName` and `hkid.ChiNameUtil` validate Chinese names and four-digit
  commercial codes.
- `hkid.EngName` and `hkid.EngNameUtil` validate English names.
- `hkid.Sex` represents the printed sex marker, currently `M` or `F`.
- `hkid.DefinedSymbol` lists supported HKID card symbols such as `***`, `A`,
  `Z`, and related markers.

## Requirements

- Java 8 or higher
- Maven 3.8 or higher for the normal build and test workflow

## Build And Test

Run the full JUnit 5 test suite:

```powershell
mvn test
```

Build the jar without rerunning tests:

```powershell
mvn -DskipTests package
```

If Maven is not installed, you can still compile and run the sample class from
PowerShell:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp out hkid.HkidTest
```

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

## Complete HKID Card Data

Use `HKID` when you need to model more than the card number:

```java
package hkid;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

public class Example {
    public static void main(String[] args) {
        HKID card = new HKID();
        card.setHkidNum(new HkidNum("A123456(3)"));
        card.setChiName(new ChiName("\u9673", "\u5927\u6587",
                Arrays.asList("1234", "5678", "9999")));
        card.setEngName(new EngName("Chan", "Tai Man"));
        card.setSex(Sex.MALE);
        card.setDateOfBirth(LocalDate.of(1990, 1, 15));
        card.setSymbols(Arrays.asList(DefinedSymbol.ThreeStars, DefinedSymbol.A));
        card.setDateOfRegistration(LocalDate.of(2005, 6, 1));
        card.setDateOfIssue(YearMonth.of(2005, 6));

        System.out.println(card.getHkidNumStr(HkidNum.Format.Complete));
        System.out.println(card.getChiName());
        System.out.println(card.getEngName());
        System.out.println(card.getSexCode());
        System.out.println(card.getAge().orElse(null));
    }
}
```

You can also generate a sample card:

```java
package hkid;

public class Example {
    public static void main(String[] args) {
        HKID card = HKIDUtil.genRandomHkid();

        System.out.println(card);
        System.out.println(card.getHkidNumStr(HkidNum.Format.Complete));
    }
}
```

## Validation Rules

- HKID number prefixes must be one or two uppercase letters.
- HKID number numerals must be exactly six digits.
- Check digits are calculated automatically and can be `0`-`9` or `A`.
- HKID number parsing rejects inputs that contain both plain and parenthesized
  check digits.
- Chinese name parts must contain Chinese characters only and have a combined
  maximum length of six characters.
- Chinese commercial codes must be four digits each. When a Chinese name is set,
  the number of commercial codes must match the number of Chinese characters.
- English name parts must start with a letter and may contain letters, spaces,
  dots, apostrophes, or hyphens.
- Sex codes are parsed with `Sex.fromCode("M")` or `Sex.fromCode("F")`.
- HKID card dates reject future values and inconsistent birth, registration,
  and issue ordering.

## Known Limitations

Some Traditional Chinese description strings in the enum reference data are
currently not corrected in this codebase. They are left unchanged here.
