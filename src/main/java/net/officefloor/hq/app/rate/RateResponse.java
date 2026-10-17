package net.officefloor.hq.app.rate;

/** A grade's rate as shown: a percent with two decimals. */
public record RateResponse(String grade, String rate) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.getGrade(), rate.getRate().setScale(2).toPlainString());
    }
}
