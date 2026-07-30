# Shared reference data

This directory contains the shared reference data used by every implementation
of HKID Utilities.

- `defined-prefixes.json` stores HKID prefix descriptions and date ranges.
- `hkid-symbols.json` stores a smart-card symbol table.
- `chinese-name-seed.csv` stores the starter data used by name generation.

The prefix and symbol tables are non-authoritative and are best treated as
internet folklore rather than verified documentation. Their contents may be
incomplete, outdated, or wrong. You may edit either file and rebuild the library
with your own values.

The structured documents carry a `schemaVersion`; their enum names, category
names, and `zh-Hant` locale key are stable machine keys. The CSV is reserved for
the genuinely tabular name seed. Language implementations should strictly
validate the schema and every entry while loading or embedding the data, so
custom versions must retain the required schema and fields. Validation rules and
domain behaviour remain in code rather than in these files.
