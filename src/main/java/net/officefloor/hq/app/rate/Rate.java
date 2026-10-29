package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A sheet's rate for a grade: a percent with two decimals, in force from {@code from} (null: from the beginning of
 * time) through {@code to} (null: with no end). {@code version} is how many versions the grade and from-date's rate has
 * had: setting it again keeps the value replaced as an older version, and this is always the newest. A forward-only
 * rate (saved with a from-date on or after the sheet's today) applies only to claims with an id above
 * {@code claimsAfter}; null applies to every claim.
 */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int version, Long claimsAfter) {

    /** A rate as typed, before it is stored: its first version. */
    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        this(sheetId, grade, rate, from, to, 1, null);
    }

    /** Whether the rate applies to the claim with the id (null: no claim, the rate in force regardless). */
    public boolean appliesTo(Long claimId) {
        return claimsAfter == null || claimId == null || claimId > claimsAfter;
    }
}
