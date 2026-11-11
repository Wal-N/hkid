package io.github.wal_n.hkid.card;

/**
 * A potentially inconsistent combination of supplied card fields.
 * These reasons are advisory and do not establish that a card is invalid.
 */
public enum HkidSuspicion {
    /** Birth is on or after the end of the prefix's birth-registration period. */
    PREFIX_BIRTH_DATE_MISMATCH,
    /** A Hong Kong birth-registration prefix accompanies a non-Hong-Kong birthplace. */
    PREFIX_BIRTH_PLACE_MISMATCH,
    /** The first-registration month does not overlap the prefix's first-issue period. */
    PREFIX_FIRST_REGISTRATION_MISMATCH
}
