package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate as the front end reads it: the percent with two decimals and the date (yyyy-MM-dd)
 *  it applies from, null when it applies from the beginning of time; and the date it applies through,
 *  null when it has no end; and the level of the sheet it is on: 0 for the sheet's own, 1 for its parent's,
 *  and so on up; and how many versions it has (the newest is the one shown). */
public record RateResponse(String grade, String pct, String from, String to, int level, int version) {
    public static RateResponse of(String grade, BigDecimal pct, LocalDate from, LocalDate to, int version) {
        return of(grade, pct, from, to, 0, version);
    }

    public static RateResponse of(String grade, BigDecimal pct, LocalDate from, LocalDate to, int level, int version) {
        return new RateResponse(grade, pct.setScale(2).toPlainString(), from == null ? null : from.toString(),
                to == null ? null : to.toString(), level, version);
    }
}
