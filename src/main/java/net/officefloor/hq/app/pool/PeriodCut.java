package net.officefloor.hq.app.pool;

/** A pool member's cut as it stood at the end of a period, with two decimals ('' for none). */
public record PeriodCut(String period, String cut) {
}
