package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate as the front end reads it: the percent with two decimals and the date (yyyy-MM-dd)
 *  it applies from, null when it applies from the beginning of time. */
public record RateResponse(String grade, String pct, String from) {
    public static RateResponse of(String grade, BigDecimal pct, LocalDate from) {
        return new RateResponse(grade, pct.setScale(2).toPlainString(), from == null ? null : from.toString());
    }
}
