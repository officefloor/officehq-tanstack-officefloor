package net.officefloor.hq.app.rollup;

/** A person's figures as shown: their claimed, due-total (two decimals; the due-total may be n/a) and overdue-total
 *  (two decimals). */
public record Figures(String name, String claimed, String dueTotal, String overdueTotal) {
}
