package net.officefloor.hq.app.rate;

/** A grade's rate on a sheet: a percent with two decimals, as text, in force from its from-date
 *  (yyyy-MM-dd; null applies from the beginning of time) through its to-date (null: no end). Its level
 *  is how far up the sheet's parent chain it comes from (0: the sheet's own, 1: its parent's, ...). */
public record Rate(String sheetId, String grade, String pct, String from, String to, int level) {

    /** Whether the rate has ended before the date (yyyy-MM-dd). */
    public boolean endedBefore(String date) {
        return to != null && to.compareTo(date) < 0;
    }

    /** The rate as inherited from that many levels up. */
    public Rate at(int level) {
        return new Rate(sheetId, grade, pct, from, to, level);
    }
}
