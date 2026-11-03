package net.officefloor.hq.app.person;

/** One of a person's past retirements: from the date they were retired from to the sheet's today when they
 *  were unretired (yyyy-mm-dd). No claim of theirs may be dated after its from-date and before its to-date. */
public record Retirement(String from, String to) {
}
