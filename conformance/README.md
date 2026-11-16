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

Java's constructors that take separate `prefix`, `numerals`, and optional
`checkDigit` arguments accept only ASCII letters and digits
in their respective fields, do not map fullwidth characters, and do not remove
outer characters or accept separators. A supplied check digit must be
unparenthesized. These are already separated components, not raw number text.

### Other domain rules

- A Chinese name character is a Unicode code point whose script is Han and
  whose general category is a letter. Han-script symbols such as Kangxi and
  CJK radicals are not name characters.
- Chinese name lengths and Chinese Commercial Code counts are measured in
  Unicode code points, not UTF-8 bytes, UTF-16 code units, or grapheme
  clusters. A supplementary-plane Han letter therefore counts as one
  character.
- Commercial codes use objects such as `{"code":"0123","extensionNumber":null}`.
  Both components are strings. An omitted or null extension means no extension,
  which is distinct from `"0"`. Value comparisons include the extension;
  display text contains only the four-digit code.
- Compare `expectReasons` in order and `expectDescription` as exact text.
  Holder descriptions use the case's `referenceDate`; a missing or null
  `locale` uses its `defaultLocale`.
- Age calculation follows section 5 of Hong Kong's
  [Age of Majority (Related Provisions) Ordinance (Cap. 410)](https://www.elegislation.gov.hk/hk/cap410!en/s5).
  A 29 February birthday has its relevant anniversary on 1 March in a
  non-leap year.
- Age-specific symbols describe the holder's age when the card was registered.
  They are not reinterpreted using a later `validateAsOf` reference date.
- Prefix periods use an inclusive start and an exclusive end. A first-issue
  month matches a prefix when at least one day in that month overlaps the
  prefix period.
- `validateAsOf` checks each dated field that is present independently; a card
  does not need every optional date field in order to validate the others.

Language-specific API design is deliberately outside this contract. Java
constructors, exception class names, builders, equality implementations, and
Rust traits do not need to match.

Random-number sequences and generated values are also outside this contract.
Different language implementations may use different random-number generators.
Random generation should be tested within each implementation by checking
output invariants, such as valid formats, check digits, date ranges, and
relationships between generated fields.
