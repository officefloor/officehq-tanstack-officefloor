package net.officefloor.hq.app.setting;

/** A requested parent sheet id; {@code value} null: blank, none. {@code given} false: left as it is. */
public record Parent(boolean given, String value) {
}
