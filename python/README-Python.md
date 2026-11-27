# HKID Utilities for Python

The Python package uses the Rust core to validate HKID numbers, names, symbols
and card fields, describe holders and generate sample data. It targets standard
CPython 3.11 and later using the stable ABI. The wheel build targets are
Windows x86-64, macOS on Apple silicon and Intel, and Linux with glibc 2.28
or later on x86-64 and ARM64.

## Quick start

Install the prebuilt wheel from [PyPI](https://pypi.org/project/hkid-utils/).

~~~shell
pip install hkid-utils
~~~

To use the API in this checkout, build and install it from the repository root
with `python -m pip install ./python`.

## Numbers and prefixes

~~~python
from hkid_utils import check_digit, format_number, is_valid

# Validation
print(is_valid("A123456(3)"))  # True
print(is_valid("A123456(4)"))  # False (expected check digit 3)

# Formatting
number = "A123456"
print(format_number(number))  # A123456(3)
print(check_digit(number))    # 3
~~~

The check digit can also be a letter. For example, `check_digit("a000002")`
returns `"A"`. Fullwidth input such as `ａ１２３４５６` formats as `A123456(3)`.

All three functions accept a string with a one- or two-letter prefix, six
digits and an optional check digit. A supplied digit must match. For invalid
text, `is_valid` returns `False`, and `check_digit` and `format_number` raise
`ValueError`. A missing number (`None`) also validates as `False`, while
`check_digit(None)` and `format_number(None)` raise `TypeError`. Other
non-string inputs raise `TypeError` for all three functions.

~~~python
from datetime import date
from hkid_utils import DefinedPrefix, HkidNumber, is_valid_check_digit

number = HkidNumber.parse_with_check_digit("Ａ１２３４５６（３）")
assert number == HkidNumber.from_parts("A", "123456")
assert number.without_check_digit == "A123456"
assert number.without_parentheses == "A1234563"
assert number.masked == "****456(*)"
assert is_valid_check_digit("A123456", "3")

transition = date(2026, 7, 27)
prefixes = DefinedPrefix.from_first_issue_month(2026, 7, earliest=transition)
assert [prefix.id for prefix in prefixes] == ["L"]
~~~

`is_valid_ascii_without_whitespace(value)` restricts validation to ASCII with
no spaces. A parsed number exposes its `prefix`, `numerals`, `check_digit` and
`defined_prefix`. Use `DefinedPrefix.lookup("WX")` to look up descriptions
and registration periods, or `from_birth_registration_date(date)` to find
the prefix used on a particular date. Both lookups return `None` for no match.

## Names, symbols and cards

~~~python
from datetime import date
from hkid_utils import ChineseCommercialCode, ChineseName, EnglishName, HkidCard, HkidSymbols

codes = [ChineseCommercialCode(code) for code in ("7115", "1129", "2429")]
card = HkidCard(
    chinese_name=ChineseName("陳", "大文", codes),
    english_name=EnglishName("Chan", "Tai Man"),
    hkid_number="Z123456",
    date_of_birth=date(2000, 1, 1),
    first_registration_year_month="2011-06",
    date_of_registration=date(2020, 6, 1),
    sex="M",
    symbols=HkidSymbols("Z *** A"),
)
reference = date(2026, 7, 21)
assert card.age_on(reference) == 26
assert str(card.symbols) == "***AZ"
assert card.find_suspicions() == ["PREFIX_BIRTH_DATE_MISMATCH"]
card.validate_as_of(reference)
print(card.describe_holder_on(reference, "zh-HK"))
~~~

Both name classes provide `is_valid` and `is_valid_part`. Commercial codes
preserve leading zeros and an optional extension, e.g. `ChineseCommercialCode("0123", "0")`.
`HkidSymbol.lookup("Z")` returns a symbol with its category and bilingual
descriptions. `HkidSymbols` supports iteration, membership tests, category
lookups and `validate_age(date_of_birth, date_of_registration)`.

All card fields are optional. Supply calendar dates as `datetime.date` and
the first-registration month as a `"YYYY-MM"` string. Construction checks
date order and eligibility for age-specific symbols at registration.
`card.replace(**changes)` constructs a validated copy. `find_suspicions()`
reports advisory mismatches in the supplied fields. For holder descriptions,
pass a BCP 47 language tag such as `"zh-HK"` or `"en"`, or omit it to use
the system locale. `describe_holder()` uses today's local date.

## Generate test data

~~~python
from datetime import date
from hkid_utils import DefinedPrefix, HkidGenerator, Sex

generator = HkidGenerator()
number = generator.number_from_prefixes([DefinedPrefix.lookup("WX")])
name = generator.name(sex=Sex.FEMALE, personal_name_length=2)
print(name.chinese, name.chinese.commercial_codes[0].code, name.english)

reference = date(2026, 7, 21)
sample = generator.card_on(reference, sex="F", min_age=18, max_age=18, birthplace="Z")
assert sample.age_on(reference) == 18
sample.validate_as_of(reference)
~~~

`number()` uses a defined prefix and `number_with_any_prefix()` allows any
one- or two-letter prefix. Generated names include commercial codes and matching
Cantonese romanisation. Inspect the bundled seed with `ChineseNameEntry.all()`.
`date_in_range(start, end)` includes the start and excludes the end, which
defaults to today. `card()` uses today and `card_on(date)` uses the supplied
date. Both default to ages 11 through 85 and sample birthplace using 2021
census weights. Set inclusive age bounds within 11 to 100 and select a
birthplace with `"Z"`, `"X"`, `"W"` or `"O"`. Generated numbers can
coincide with real HKIDs and are intended for isolated test data.

## Install a CI wheel

On GitHub, open a successful **Python CI** run under **Actions**. Download the
`.whl` for your operating system and CPU from **Artifacts**, then run
`python -m pip install <path-to-wheel>` with the downloaded file's path.
CI installs each wheel on its native runner and runs the Python tests on both
CPython 3.11 and the latest stable CPython.

## Build and test locally

From the repository root, use an active Python virtual environment with the
repository's Rust toolchain and a native linker to build the wheel.

~~~shell
python -m pip wheel ./python --no-deps --wheel-dir target/wheels
~~~

In a fresh virtual environment, install the local wheel and run the tests
against the shared number, name, symbol and card fixtures.

~~~shell
python -m pip install --no-index --find-links target/wheels hkid-utils
python -m unittest discover -s python/tests -v
~~~
