package net.officefloor.hq.app.rate;

import java.util.List;

/** A rate as sent to the screen: the percent as text with two decimals, so it is never rounded through a float;
 *  from is its from-date (yyyy-MM-dd), null when it applies from the beginning of time; to its to-date, null when it has no end;
 *  version how many versions it has had (it being the newest); level is how many levels up the parent chain its sheet is (0: the sheet's own rate, 1: its parent's ...);
 *  after is set when it is forward-only: it applies only to claims whose seq is above it (null: to every claim; see
 *  Rate#appliesTo); older are the versions it replaced, newest first, which still apply to the claims it does not. */
public record RateResponse(String sheetId, String grade, String rate, String from, String to, int version, int level,
        Long after, List<Older> older) {

    /** An older version of a rate: its percent, to-date and after as for the rate. */
    public record Older(String rate, String to, int version, Long after) {
        static Older from(Rate rate) {
            return new Older(rate.rate().toPlainString(), rate.to() == null ? null : rate.to().toString(),
                    rate.version(), rate.after());
        }
    }

    public static RateResponse from(Rate rate, int level, List<Rate> older) {
        return new RateResponse(rate.sheetId(), rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString(), rate.to() == null ? null : rate.to().toString(),
                rate.version(), level, rate.after(), older.stream().map(Older::from).toList());
    }
}
