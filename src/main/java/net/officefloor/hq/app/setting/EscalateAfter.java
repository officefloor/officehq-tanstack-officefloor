package net.officefloor.hq.app.setting;

/** A validated escalation gap, whole days; {@code value} null: blank, off. {@code given} false: left as it is. */
public record EscalateAfter(boolean given, Integer value) {
}
