package net.officefloor.hq.app.setting;

/** The validated score log switch; {@code on} false: blank. {@code given} false: left as it is. */
public record ScoreLog(boolean given, boolean on) {
}
