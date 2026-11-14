package net.officefloor.hq.app.setting;

/** A validated default grade (A, B or C); {@code value} null: blank, A. {@code given} false: left as it is. */
public record DefaultGrade(boolean given, String value) {
}
