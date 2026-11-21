# Cross-language conformance fixtures

The JSON files in this directory define deterministic domain behaviour shared
by every HKID Utilities implementation.

Each fixture document has a `schemaVersion`. Test adapters must reject an
unsupported schema version instead of silently interpreting it differently.
Case `id` values are stable identifiers intended for test output and code
review.

In number and symbol `parseCases` and `invalidParseCases`, the `input` field
is required. In `checkDigitValidationCases`, both `input` and `checkDigit`
are required. Each must contain a JSON string or explicit JSON `null`.
Missing fields or other JSON types are malformed fixtures and must fail the
test adapter; they must not be treated as invalid domain inputs or coerced
to strings.

The fixtures cover:

- parsing, normalization, validation, and formatting;
- check-digit calculation and validation;
- canonical smart-card symbol ordering and age rules;
- Chinese and English name validation and formatting;
- Chinese Commercial Code extensions, value distinctions, and display;
- complete-card formatting and consistency rules;
- ordered suspicion reasons and localized holder descriptions.

## Normative behaviour details

These rules define operations and observable results, not API shapes. A language
may use constructors, functions, optional values, or result types to expose them.
Accepting partial card data and validating a complete field are separate
operations; acceptance does not imply that every validation predicate is true.

### Missing, null, and empty values

There is no global rule equating a missing JSON field, `null`, and `""`.
Required fixture fields must be present even when the domain input is null.
In `englishNameCases` and `invalidEnglishNameCases`, `surname` and `personalName`
are required strings or explicit nulls. Optional `expectSurname` and
`expectPersonalName` give normalized results; when absent, the corresponding
input string is the expected result. A null input part needs an explicit
expected string. In `englishNameValidationCases`, either input part may be
omitted and has the same meaning as an explicit null.

| Context | Meaning |
| --- | --- |
| Number text or separate body/check-digit validation | Null or empty after the specified outer removal is invalid. |
| Symbol text parsing | Null is invalid; empty after outer removal is an empty collection. |
| Chinese or English name representation | A null part becomes `""`; empty and partial names are allowed subject to syntax and length rules below. |
| Optional fields in a card fixture's `input` | Missing or null number, sex, or date means absent; missing or null name parts mean `""`; missing or null symbols or commercial codes mean an empty collection. Explicit empty number, sex, or date strings are invalid. |
| Commercial-code `extensionNumber` | Missing or null means no extension; `""` is invalid; `"0"` is a present extension. |
| Holder-description `locale` | Missing or null selects the case's `defaultLocale`; at least one must be supplied. |

Card fixture defaults are applied before calling the language-specific API.
They do not require a direct symbol parser to accept null. Absent card number,
sex, and dates have null formatted results; empty names and symbols format as
`""`. An absent birth date yields no age, not zero. Explicit reference-date
arguments must be non-null; null does not request today's date.

### Number and symbol text parsing

Number and symbol text parsers use the following explicit character sets.
Implementations must not substitute a language's default `trim`, whitespace
predicate, or regular-expression `\s` for these sets.

| Use | Allowed characters |
| --- | --- |
| Remove from the beginning and end of an input | TAB `U+0009`, LF `U+000A`, CR `U+000D`, SPACE `U+0020`, NO-BREAK SPACE `U+00A0`, IDEOGRAPHIC SPACE `U+3000` |
| Separate complete components inside an input | SPACE `U+0020`, NO-BREAK SPACE `U+00A0`, IDEOGRAPHIC SPACE `U+3000` |

Remove the longest leading and trailing runs of the first set. Inside the
remaining text, zero or more characters from the second set may occur only
at the component boundaries described below. TAB, CR, and LF are therefore
allowed at the outside but rejected inside an input. Other control characters
(including NUL `U+0000`, VT `U+000B`, and FF `U+000C`), Unicode spaces,
zero-width characters, and a byte-order mark are not removed or treated as
separators; their presence makes these inputs invalid.

Before validating number or symbol text, map only the following fullwidth
characters to their ASCII equivalents, one character at a time:

| Input range | ASCII output range |
| --- | --- |
| `U+FF10` through `U+FF19` (`０`–`９`) | `U+0030` through `U+0039` (`0`–`9`) |
| `U+FF21` through `U+FF3A` (`Ａ`–`Ｚ`) | `U+0041` through `U+005A` (`A`–`Z`) |
| `U+FF41` through `U+FF5A` (`ａ`–`ｚ`) | `U+0061` through `U+007A` (`a`–`z`) |

ASCII and fullwidth letters or digits may be mixed in one input. This mapping
applies to number parsing and its boolean validation, separate body/check-digit
validation, prefix lookup, symbol-string parsing and single-symbol lookup.
It does not map punctuation, spaces, or other Unicode letters or digits.
In particular, do not use general Unicode normalization such as NFKC, and
do not fold fullwidth parentheses before checking their pairing. Arabic-Indic,
circled, superscript and mathematical digits are invalid, as are unsupported
letters such as `ß`. Existing whitespace rules still apply after mapping.

- After the mapping, an HKID number consists of a prefix of one or two ASCII
  letters, six consecutive ASCII digits (`0` through `9`), and an optional
  check digit.
  The check digit is one ASCII digit or `A`/`a`, either bare or immediately
  enclosed in a matching pair of ASCII parentheses (`U+0028` / `U+0029`)
  or fullwidth parentheses (`U+FF08` / `U+FF09`). Mixing the two widths is
  invalid. Separators may occur between the prefix and the six digits, and
  between the six digits and the complete check-digit component. They must
  not split a two-letter prefix, the six digits, or a
  parenthesized check digit. Thus `A 123456(3)`, `AB 123456 (9)`, and
  `A123456 3` are accepted, while `A B123456(9)`, `A12 3456(3)`, and
  `A123456( 3 )` are rejected. `A 123456 （3）` is accepted, while
  `A123456（3)`, `A123456(3）`, and `A123456（ 3 ）` are rejected.
- Check-digit validation of a separate body and check digit applies the
  outer-removal and fullwidth-mapping rules to each input independently.
  The body accepts separators between its prefix and six digits; the separate
  check digit must be a single ASCII digit or `A`/`a` after mapping, without
  either kind of parentheses. A supplied check digit must match even when
  separators were present.
- Number prefixes and check-digit letters normalize ASCII lowercase to
  uppercase after the explicit fullwidth mapping and format validation.
  For example, `Ａ１２３４５６（３）` and `Ａ12３456(３)` both format as
  `A123456(3)`, and `ａ０００００２（ａ）` formats as `A000002(A)`.
  Other non-ASCII letters and digits are rejected. Fullwidth-parenthesis
  support applies only to the matching pair enclosing the check digit;
  other Unicode punctuation is not normalized.
  Formatted numbers contain no whitespace separators and use ASCII parentheses
  whenever parentheses are included, regardless of the input form.
- A symbol string consists of complete current symbol codes, with optional
  separators between them. The three adjacent asterisks in `***` form one
  complete symbol. `*** A Z` is accepted and formats as `***AZ`; `* * *`
  contains three separate single-star symbols and is rejected, not joined
  into `***`. Existing duplicate-symbol and mutually exclusive-symbol rules
  still apply after parsing. Fullwidth symbol letters map to ASCII before
  ASCII lowercase letters normalize to uppercase, and output uses canonical
  symbol order without separators. `*** Ａ ｚ` formats as `***AZ`, but fullwidth
  asterisks are invalid. Digits are not supported symbol codes in either
  width.
- Prefix lookup and single-symbol parsing use the same outer-removal rule,
  but do not accept separators inside the prefix or single symbol.
- An input that becomes empty is invalid for number parsing and check-digit
  validation. An empty symbol string represents the empty symbol collection.
  Null inputs are invalid for number and symbol parsing and check-digit
  validation.

When accepting already separated `prefix`, `numerals`, and optional
`checkDigit` components, require ASCII letters and digits without trimming,
width mapping, or separators. Normalize lowercase letters to uppercase.
A null or empty optional check digit means derive it; a non-empty one must be
unparenthesized and match. This differs from the separate body/check-digit
validation operation, which requires both values.

### Check digits, errors, and number output

Map letters `A` through `Z` to 10 through 35. For a one-letter prefix, prepend
a padding value of 36. The resulting two prefix values followed by the six
numeral values have weights `9, 8, 7, 6, 5, 4, 3, 2`. Let `S` be their weighted
sum and `d = (11 - (S mod 11)) mod 11`. Render 10 as `A`, otherwise render the
digit `0` through `9`. For example, `A123456` gives `S = 481` and check digit
`3`; `AB123456` gives `S = 255` and check digit `9`. Leading numeral zeros
are preserved. An omitted check digit is derived; any supplied one is checked.

Number parse failures have two shared categories:

- `INVALID_FORMAT`: null, empty, or syntactically invalid number text, including
  an unsupported check-digit character or malformed parentheses.
- `INVALID_CHECK_DIGIT`: the complete syntax is accepted, but the supplied
  check digit does not match the calculation.

Boolean number/symbol validators return false for invalid domain input.
Other invalid fixture cases require rejection without prescribing an exception
class or message. Malformed fixtures are adapter errors, not domain results.

Number formats are `prefix + numerals`, `prefix + numerals + checkDigit`, or
`prefix + numerals + "(" + checkDigit + ")"` (the default). Masking replaces
each prefix letter, the first three numerals, and the check digit with `*`,
preserving the last three numerals and parentheses: `A123456(3)` becomes
`****456(*)`, and `AB123456(9)` becomes `*****456(*)`.

### Names and commercial codes

- Name parts are not trimmed, case-folded, or width-normalized.
- A non-empty English name part consists of ASCII letter groups separated by
  exactly one ASCII space, period, apostrophe, or hyphen. Leading, trailing,
  or adjacent separators are invalid. Syntax-only name-part validation rejects
  null and empty input and does not impose a length limit.
- An English name representation accepts either or both parts empty, including
  null normalized to empty. Format two non-empty parts as `surname, personalName`;
  otherwise use the one non-empty part or `""`. The formatted length, including
  the comma and space, must be at most 40 characters.
- Complete English-name validation additionally requires a non-empty surname.
  Thus `(null, "Tai Man")` is representable as `"Tai Man"` but validates false;
  `("Chan", null)` is representable as `"Chan"` and validates true.
- A Chinese name character is a Unicode code point whose script is Han and
  whose general category is a letter. Han-script symbols such as Kangxi and
  CJK radicals are not name characters.
- Chinese name lengths and Chinese Commercial Code counts are measured in
  Unicode code points, not UTF-8 bytes, UTF-16 code units, or grapheme
  clusters. A supplementary-plane Han letter therefore counts as one
  character.
- The Han repertoire follows the implementation's Unicode character data;
  this contract does not pin a Unicode version. Newly added Han letters may
  therefore differ between runtimes, beyond the characters in the fixtures.
- Chinese name representation and validation both allow empty or partial names,
  with a maximum combined length of six code points. Format the surname followed
  immediately by the personal name, without a separator.
- Commercial codes use objects such as `{"code":"0123","extensionNumber":null}`.
  The code is exactly four ASCII digits; a present extension is exactly one
  ASCII digit. Preserve leading zeros. Value comparisons include the extension;
  display text contains only the four-digit code and is not a lossless encoding.
- Null or empty commercial-code collections mean no codes. A non-empty
  collection must contain one non-null code per Chinese name code point, in
  printed-name order. Validation checks structure and count, not whether a code
  is assigned to the corresponding character.

### Symbols and output ordering

- Canonical symbol order is `***`, `*`, `A`, `C`, `R`, `U`, `Z`, `X`, `W`, `O`,
  `B`, `N`, independent of input order. Emit only present symbols without
  separators. Duplicates are invalid. At most one member of each group
  `{***, *}`, `{A, C, R, U}`, and `{Z, X, W, O}` may be present; `B` and `N`
  may coexist.
- `***` requires age 18 or above; `*` requires age 11 through 17, inclusive.
  Neither symbol is required merely because a holder is in one of those ranges.
- Suspicion reasons are distinct and ordered as `PREFIX_BIRTH_DATE_MISMATCH`,
  `PREFIX_BIRTH_PLACE_MISMATCH`, `PREFIX_FIRST_REGISTRATION_MISMATCH`. They are
  advisory and do not prevent accepting a card. Skip a check when its required
  fields or prefix metadata are absent. A card with no applicable checks has
  no suspicion reasons.
- For Hong Kong birth-registration prefixes, a birth on or after the period's
  exclusive end triggers `PREFIX_BIRTH_DATE_MISMATCH`; an explicit `X`, `W`, or
  `O` birthplace triggers `PREFIX_BIRTH_PLACE_MISMATCH`. Birth before the period
  starts is allowed because no maximum birth-registration delay is assumed.
  Symbol `B` suppresses both birth checks.
- `PREFIX_FIRST_REGISTRATION_MISMATCH` means the first-registration month does
  not overlap a known first-issue period. Ignore the current-card registration
  date for this check. Prefix `L` is exempt because its metadata omits historical
  reserve use; symbol `B` does not suppress this check.
- Commercial-code collections retain printed-name order; do not sort them.
  Compare all expected ordered collections in order and formatted strings,
  including `expectDescription`, as exact text.

### Dates, card consistency, and holder descriptions

- Dates are calendar dates, not instants. Fixture dates use `YYYY-MM-DD` and
  first-registration months use `YYYY-MM`. An explicit reference date controls
  the operation without consulting today's date or the host time zone.
- Convenience operations that omit a reference date use the current system
  date in the runtime's default time zone, resolved on each call. This applies
  to holder summaries, random-card generation (with or without a requested sex),
  and the default end of random-date generation. Changing the default time zone
  may change the date and result. Callers needing a specific business date,
  including Hong Kong's date, must supply it explicitly. Shared deterministic
  cases provide dates explicitly; test default-date behavior within each
  implementation.
- Random-date ranges include their start and exclude their end; the default
  end is the current system date. Equal or reversed endpoints are invalid.
  Random-card generation may include its reference date as the registration
  date and must produce a card valid as of that date.
- Age calculation follows section 5 of Hong Kong's
  [Age of Majority (Related Provisions) Ordinance (Cap. 410)](https://www.elegislation.gov.hk/hk/cap410!en/s5).
  A 29 February birthday has its relevant anniversary on 1 March in a
  non-leap year.
- Age is completed years on the reference date; a reference date before birth
  is invalid. Card construction permits missing fields, but present dates must
  be consistent: current-card registration is on or after `2018-11-26` and not
  before birth; the first-registration month is not before the birth month or
  after the current-card registration month.
- Age-specific symbols describe the holder's age when the card was registered.
  Card construction checks them only when both birth and current-card
  registration dates are present; standalone symbol-age validation requires
  both dates. They are not reinterpreted using a later `validateAsOf` reference date.
- Prefix periods use an inclusive start and an exclusive end. A first-issue
  month matches a prefix when at least one day in that month overlaps the
  prefix period.
- `validateAsOf` checks each dated field that is present independently; a card
  does not need every optional date field in order to validate the others.
  Birth and current-card registration must not be after the reference date;
  the first-registration month must not be after its calendar month.
- Printed birth dates use `DD-MM-YYYY`, first-registration months `MM-YY`, and
  current-card registration dates `DD-MM-YY`, with zero-padded components.
  Sex markers format as `男 M` or `女 F`.
- Holder descriptions include only available age, sex, reported birthplace,
  and symbol meanings. Omit missing information; return `""` if nothing can be
  described. Do not infer facts from a prefix or absent symbols. Names, codes,
  exact birth dates, numbers, and registration dates are excluded. A locale
  whose language is `zh` selects Traditional Chinese; all other languages select
  English. Default locale is resolved on each call. Exact wording is specified
  by the holder-description fixtures.

### Language-specific APIs and random generation

Language-specific API design is deliberately outside this contract. Java
constructors, exception class names, builders, equality implementations, and
Rust traits do not need to match.

Random-number sequences and generated values are also outside this contract.
Different language implementations may use different random-number generators.
Random generation should be tested within each implementation by checking
output invariants, such as valid formats, check digits, date ranges, and
relationships between generated fields.
