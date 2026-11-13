package net.officefloor.hq.app.rate;

/** A rate as the screen shows it: the grade, the percent with two decimals and the from-date (null: from the start). */
public record RateResponse(String grade, String rate, String from) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().setScale(2).toPlainString(),
                rate.from() == null ? null : rate.from().toString());
    }
}
