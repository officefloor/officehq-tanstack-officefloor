package net.officefloor.hq.app.rate;

/** A rate as the screen shows it: the grade and the percent with two decimals. */
public record RateResponse(String grade, String rate) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().setScale(2).toPlainString());
    }
}
