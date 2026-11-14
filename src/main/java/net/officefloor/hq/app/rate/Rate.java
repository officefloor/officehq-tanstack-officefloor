package net.officefloor.hq.app.rate;

/** A grade's rate on a sheet: a percent with two decimals, as text. */
public record Rate(String sheetId, String grade, String pct) {
}
