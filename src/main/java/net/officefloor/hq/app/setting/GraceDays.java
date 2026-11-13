package net.officefloor.hq.app.setting;

/** Validated grace days, whole days; {@code value} null: blank, off. {@code given} false: left as it is. */
public record GraceDays(boolean given, Integer value) {
}
