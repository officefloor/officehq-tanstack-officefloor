package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A grade's rate as the front end reads it: the percent with two decimals. */
public record RateResponse(String grade, String pct) {
    public static RateResponse of(String grade, BigDecimal pct) {
        return new RateResponse(grade, pct.setScale(2).toPlainString());
    }
}
