package net.officefloor.hq.app.setting;

/** A validated fiscal year start month, 1 to 12 (blank: back to 1). {@code given} false: left as it is. */
public record FiscalStartMonth(boolean given, int value) {
}
