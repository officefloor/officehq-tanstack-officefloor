package net.officefloor.hq.app.plan;

/** A person's plan: an amount (two decimals), from {@code start} every {@code every} days (or, when {@code day} is set,
 *  monthly on that day of the month) to {@code end} (ISO dates),
 *  with how many claims it has {@code generated} so far. */
public record Plan(long id, long person, String amount, String start, int every, String end, int generated, Integer day) {
}
