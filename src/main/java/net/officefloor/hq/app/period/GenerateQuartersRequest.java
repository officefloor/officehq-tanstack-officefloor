package net.officefloor.hq.app.period;

/** The calendar year (yyyy) to generate the quarterly periods of, and the signed-in user (`by`) generating them. */
public record GenerateQuartersRequest(String year, String by) {
}
