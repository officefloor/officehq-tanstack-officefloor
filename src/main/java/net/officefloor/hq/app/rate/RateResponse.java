package net.officefloor.hq.app.rate;

/** A grade's rate as the screen shows it: the percent with two decimals and its from-date and to-date (null when none). */
public record RateResponse(String grade, String pct, String from, String to) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.getGrade(), rate.getPct().setScale(2).toPlainString(),
                rate.getFromDate() == null ? null : rate.getFromDate().toString(),
                rate.getToDate() == null ? null : rate.getToDate().toString());
    }
}
