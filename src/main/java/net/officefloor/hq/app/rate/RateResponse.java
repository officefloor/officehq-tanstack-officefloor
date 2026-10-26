package net.officefloor.hq.app.rate;

/** A rate as the screen sees it: {@code rate} has two decimals ("12.50"); {@code from} is yyyy-mm-dd or null. */
public record RateResponse(String grade, String rate, String from) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString());
    }
}
