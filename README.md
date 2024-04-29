# HKIDNumber Library

The `HKIDNumber` library provides a comprehensive solution for generating, validating, and formatting Hong Kong Identity Card (HKID) numbers according to the official specifications. It is designed for use in Java applications that require handling HKID numbers, offering features like custom exceptions for invalid formats and check digits, and methods for random HKID number generation.

## Features

- Validate HKID numbers for correct format and check digit.
- Generate HKID numbers with specific or random values.
- Format HKID numbers with or without check digits, including parentheses formatting.
- Throw custom exceptions for invalid HKID formats and check digits.

## Requirements

- Java 8 or higher

## Usage

Below are some examples of how to use the HKIDNumber library in your projects.

### Validating an HKID Number
```java
try {
    HKIDNumber hkid = new HKIDNumber("A123456(7)");
    System.out.println("HKID is valid.");
} catch (InvalidHKIDNumberFormatException | InvalidCheckDigitException e) {
    System.out.println("Invalid HKID: " + e.getMessage());
}
```
Or
```java
if (HKIDNumber.validateCheckDigit("A123456", "3")){
        System.out.println("Check Digit is correct.");
}
```

### Generating a Random HKID Number
```java
HKIDNumber randomHkid = HKIDNumber.genRandomHkidNumber();
System.out.println("Random HKID: " + randomHkid.toString());
```

### Formatting an HKID Number
```java
HKIDNumber hkid = new HKIDNumber("A123456(7)");
System.out.println("Formatted HKID: " + hkid.toString(HKIDNumber.Format.Complete));
```

## Contributing
Contributions are welcome! Please feel free to submit pull requests, report bugs, or suggest features through the GitHub issue tracker.

## License
This project is licensed under the MIT License. See the LICENSE file for details.