package net.officefloor.hq.app.rate;

/** A rate as the screen sees it: `pct` with two decimals, `from` as yyyy-mm-dd or null when it has none. */
public record RateResponse(String grade, String pct, String from) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() != null ? rate.from().toString() : null);
    }
}
