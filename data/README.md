# Shared reference data

This directory contains the shared reference data used by every implementation
of HKID Utilities.

- `defined-prefixes.json` stores HKID prefix descriptions and date ranges.
- `hkid-symbols.json` stores a smart-card symbol table.
- `chinese-name-seed.csv` stores the starter data used by name generation. Its
  `sexAssociation` field is `MALE`, `FEMALE`, or `UNISEX`. Surname rows use
  `UNISEX` because surnames are shared.

The prefix and symbol tables are non-authoritative and are best treated as
internet folklore rather than verified documentation. Their contents may be
incomplete, outdated, or wrong. You may edit either file and rebuild the library
with your own values.

The structured documents carry a `schemaVersion`; their enum names, category
names, and `zh-Hant` locale key are stable machine keys. The CSV is reserved for
the genuinely tabular name seed. Language implementations should strictly
validate the schema and every entry while loading or embedding the data, so
custom versions must retain the required schema and fields. When a male or
female name is requested, implementations may select matching or `UNISEX`
given-name entries. `UNISEX` maps to support for both card sex values rather
than to a third public enum value. Classification describes a generation
tendency, not an intrinsic property of a real person's name. Validation rules
and domain behaviour remain in code rather than in these files.

Date periods use an inclusive `start` and an exclusive `endExclusive`; a null
`endExclusive` represents an open-ended period. A first-issue month overlaps a
period when at least one day in that calendar month falls inside the period.
