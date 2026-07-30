# Cross-language conformance fixtures

The JSON files in this directory define deterministic domain behaviour shared
by every HKID Utilities implementation.

Each fixture document has a `schemaVersion`. Test adapters must reject an
unsupported schema version instead of silently interpreting it differently.
Case `id` values are stable identifiers intended for test output and code
review.

The fixtures cover:

- parsing, normalization, validation, and formatting;
- check-digit calculation and validation;
- canonical smart-card symbol ordering and age rules;
- Chinese and English name validation and formatting;
- complete-card formatting and consistency rules.

Language-specific API design is deliberately outside this contract. Java
constructors, exception class names, builders, equality implementations, and
Rust traits do not need to match.

Random-number sequences and generated values are also outside this contract.
Different language implementations may use different random-number generators.
Random generation should be tested within each implementation by checking
output invariants, such as valid formats, check digits, date ranges, and
relationships between generated fields.
