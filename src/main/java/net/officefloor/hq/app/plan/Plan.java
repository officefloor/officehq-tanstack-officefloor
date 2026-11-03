package net.officefloor.hq.app.plan;

/** A person's plan: an amount (two decimals), a start date, every N days and an end date (yyyy-mm-dd), and how
 *  many claims it has made so far. */
public record Plan(long id, long personId, String amount, String start, int every, String end, int generated) {
}
