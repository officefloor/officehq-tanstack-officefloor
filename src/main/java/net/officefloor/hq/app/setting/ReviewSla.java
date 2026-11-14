package net.officefloor.hq.app.setting;

/** A validated review SLA, whole days; {@code value} null: blank, none. {@code given} false: left as it is. */
public record ReviewSla(boolean given, Integer value) {
}
