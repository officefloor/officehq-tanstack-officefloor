package net.officefloor.hq.app.setting;

/** A validated pending SLA, whole days; {@code value} null: blank, none. {@code given} false: left as it is. */
public record PendingSla(boolean given, Integer value) {
}
