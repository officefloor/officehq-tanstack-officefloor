package net.officefloor.hq.app.rate;

/** A rate as the screen shows it: the grade and the percent with two decimals. */
public record RateResponse(String grade, String rate) {
    public static RateResponse from(Rate r) {
        return new RateResponse(r.grade(), r.pct().toPlainString());
    }
}
