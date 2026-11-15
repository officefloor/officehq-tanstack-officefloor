package net.officefloor.hq.app.rate;

/** What a candidate rate for a grade would change: how many open claims would show a different due,
 *  and the sum of the differences (new due minus current due, two decimals, negative when lower). */
public record RatePreview(int count, String diff) {
}
