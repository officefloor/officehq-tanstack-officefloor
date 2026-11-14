package net.officefloor.hq.app.rate;

/** A grade's rate on a sheet: a percent with two decimals, as text, in force from its from-date
 *  (yyyy-MM-dd; null applies from the beginning of time) through its to-date (null: no end). */
public record Rate(String sheetId, String grade, String pct, String from, String to) {

    /** Whether the rate has ended before the date (yyyy-MM-dd). */
    public boolean endedBefore(String date) {
        return to != null && to.compareTo(date) < 0;
    }
}
