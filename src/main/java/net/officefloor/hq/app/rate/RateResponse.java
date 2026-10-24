package net.officefloor.hq.app.rate;

import java.util.List;

/** A grade's rate as the screen shows it: the percent with two decimals and its from-date and to-date (null when none),
 *  the sheet it is set on (the sheet's own, or one up its chain of parents), how many versions it has, and each time
 *  it was set, in order (so a claim takes the last that applies to it: see {@link Rate#forClaim}). */
public record RateResponse(String grade, String pct, String from, String to, String sheet, int version,
        List<Layer> layers) {

    /** One time the rate was set: its percent and to-date then, and, when forward-only, the highest claim id there was
     *  then (null: it applies to every claim). */
    public record Layer(String pct, String to, Long after) {
    }

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.getGrade(), rate.getPct().setScale(2).toPlainString(),
                rate.getFromDate() == null ? null : rate.getFromDate().toString(),
                rate.getToDate() == null ? null : rate.getToDate().toString(), rate.getSheetId(), rate.getVersion(),
                rate.getLayers().stream().map(l -> new Layer(l.getPct().setScale(2).toPlainString(),
                        l.getToDate() == null ? null : l.getToDate().toString(), l.getForwardAfter())).toList());
    }
}
