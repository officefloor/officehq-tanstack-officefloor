package net.officefloor.hq.app.rate;

/** A sheet's rate for a grade: a percent with two decimals, in force from its from-date (yyyy-mm-dd;
 *  null when it applies from the beginning of time) through its to-date (null when it has no end), and its
 *  version: how many versions the grade's rate from that from-date has had (1 for the first; this is the newest). */
public record Rate(String grade, String pct, String from, String to, int version) {
}
