package net.officefloor.hq.app.rate;

/** A grade's rate as the screen shows it: the percent with two decimals and its from-date and to-date (null when none),
 *  the sheet it is set on (the sheet's own, or one up its chain of parents), and how many versions it has. */
public record RateResponse(String grade, String pct, String from, String to, String sheet, int version) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.getGrade(), rate.getPct().setScale(2).toPlainString(),
                rate.getFromDate() == null ? null : rate.getFromDate().toString(),
                rate.getToDate() == null ? null : rate.getToDate().toString(), rate.getSheetId(), rate.getVersion());
    }
}
