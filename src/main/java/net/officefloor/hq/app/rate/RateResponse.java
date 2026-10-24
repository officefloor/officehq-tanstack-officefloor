package net.officefloor.hq.app.rate;

/** A grade's rate as the screen shows it: the percent with two decimals. */
public record RateResponse(String grade, String pct) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.getGrade(), rate.getPct().setScale(2).toPlainString());
    }
}
