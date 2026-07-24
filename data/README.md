# Shared reference data

This directory is the language-neutral source of truth for reference data used
by every implementation of HKID Utilities.

- `defined-prefixes.json` stores predefined HKID prefix descriptions and exact
  date ranges.
- `hkid-symbols.json` stores the current smart-card symbol table.
- `chinese-name-seed.csv` stores the starter data used by name generation.

The structured JSON documents carry a `schemaVersion`; their enum names,
category names, and `zh-Hant` locale key are stable machine keys. The CSV is
reserved for the genuinely tabular name seed. Language implementations should
strictly validate the schema and every entry while loading or embedding the
data. Validation rules and domain behaviour remain in code rather than in these
files.
