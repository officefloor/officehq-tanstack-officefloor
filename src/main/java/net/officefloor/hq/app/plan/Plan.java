package net.officefloor.hq.app.plan;

/** A person's plan: an amount (two decimals), a start date, every N days or monthly on a day of the month (1 to 31; the other null), an end date (yyyy-mm-dd), and how
 *  many claims it has made so far. */
public record Plan(long id, long personId, String amount, String start, Integer every, Integer day, String end, int generated) {
}
