package net.officefloor.hq.app.rate;

import java.util.List;

/** A grade's rate, a percent shown with two decimals ("12.50"), and the date it applies from
 *  (YYYY-MM-DD; "" when it applies from the beginning of time) and through ("" when it has no end), and its
 *  version: how many times a rate has been set for the grade and from-date, this the newest. afterMark is 0 for a rate
 *  applying to every claim, else the mark of a forward-only one: it applies only to claims with a greater mark; earlier
 *  are the older versions it replaced, newest first, for the claims it does not apply to. */
public record RateResponse(String grade, String rate, String from, String to, int version, long afterMark,
        List<Earlier> earlier) {

    /** An older version of a rate: its percent, to-date ("" for no end) and mark (see {@link RateResponse}). */
    public record Earlier(String rate, String to, long afterMark) {
    }
}
