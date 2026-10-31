package net.officefloor.hq.app.rate;

/** A rate as the screen sees it: `pct` with two decimals. */
public record RateResponse(String grade, String pct) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString());
    }
}
