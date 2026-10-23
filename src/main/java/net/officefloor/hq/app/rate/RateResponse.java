package net.officefloor.hq.app.rate;

import java.util.List;

/** A rate as the screen shows it: the grade, the percent with two decimals, and the from-date
 *  (yyyy-mm-dd, or null when it applies from the beginning of time) and the to-date (yyyy-mm-dd, or null when it has no end), and the level it comes from (0: the sheet's own; 1: its parent's,
 *  2: the parent's parent's, and so on), and its version count (how many have been set for the grade from that from-date);
 *  and, when forward-only, the highest claim id when it was saved (after; null: it reaches every claim), and the older
 *  versions of the grade's rate from that from-date, newest first (a claim it does not reach uses the newest of them
 *  that does). */
public record RateResponse(String grade, String rate, String from, String to, int level, int version, Long after,
        List<Older> older) {

    /** An older version of a rate: its percent, to-date and, when forward-only, the highest claim id when it was saved. */
    public record Older(String rate, String to, Long after) {
        static Older from(Rate r) {
            return new Older(r.pct().toPlainString(), r.to() != null ? r.to().toString() : null, r.after());
        }
    }

    public static RateResponse from(Rate r) {
        return from(r, 0, List.of());
    }

    public static RateResponse from(Rate r, int level, List<Rate> older) {
        return new RateResponse(r.grade(), r.pct().toPlainString(), r.from() != null ? r.from().toString() : null,
                r.to() != null ? r.to().toString() : null, level, r.version(), r.after(),
                older.stream().map(Older::from).toList());
    }
}
