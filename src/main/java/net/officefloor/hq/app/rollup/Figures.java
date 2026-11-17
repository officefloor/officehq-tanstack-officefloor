package net.officefloor.hq.app.rollup;

/** A person's figures as shown: their claimed, due-total (two decimals; the due-total may be n/a) and overdue-total
 *  (two decimals), and their score (a whole number: the figures of their settled claims that count / 100, minus 5 for
 *  every overdue claim, rounded once, half to even). */
public record Figures(String name, String claimed, String dueTotal, String overdueTotal, String score) {
}
