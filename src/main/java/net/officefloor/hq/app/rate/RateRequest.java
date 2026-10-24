package net.officefloor.hq.app.rate;

/** The rate form as submitted: a grade, its percent and its from-date (blank: from the beginning of time)
 *  and its to-date (blank: no end). */
public record RateRequest(String grade, String pct, String from, String to) {
}
