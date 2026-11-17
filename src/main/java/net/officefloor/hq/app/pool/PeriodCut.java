package net.officefloor.hq.app.pool;

/** A pool member's cut as it stood at the end of a period (its name): two decimals, or null when there was none. */
public record PeriodCut(String period, String cut) {
}
