package net.officefloor.hq.app.rate;

/** A grade's rate on a sheet: a percent with two decimals, as text, in force from its from-date
 *  (yyyy-MM-dd; null applies from the beginning of time). */
public record Rate(String sheetId, String grade, String pct, String from) {
}
