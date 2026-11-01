package net.officefloor.hq.app.rate;

/**
 * What a candidate rate for a grade would do: the count of OPEN and PENDING claims whose due would differ, and the
 * sum of the differences (new due minus current due, two decimals; negative when lower).
 */
public record RatePreview(int count, String diff) {
}
