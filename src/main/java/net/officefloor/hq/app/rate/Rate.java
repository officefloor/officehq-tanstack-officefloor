package net.officefloor.hq.app.rate;

/** A sheet's rate for a grade: a percent with two decimals, in force from its from-date (yyyy-mm-dd;
 *  null when it applies from the beginning of time) through its to-date (null when it has no end), and its
 *  version: how many versions the grade's rate from that from-date has had (1 for the first; this is the newest), and
 *  {@code after}: for a forward-only rate (saved with a from-date on or after the sheet's today), the claim-added number
 *  it was saved at — it applies only to claims added after it (a higher number); null when it applies to every claim.
 *  {@code superseded}: an older version kept for the claims it still applies to (listed in {@link Rates#earlier}). */
public record Rate(String grade, String pct, String from, String to, int version, Long after, boolean superseded) {
}
