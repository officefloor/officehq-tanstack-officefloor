package net.officefloor.hq.app.setting;

/** A validated maximum of open claims per person, a whole number; {@code value} null: blank, none. {@code given} false: left as it is. */
public record MaxOpen(boolean given, Integer value) {
}
