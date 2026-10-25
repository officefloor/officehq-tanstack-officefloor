package net.officefloor.hq.app.period;

/** The from and to dates to generate monthly periods between, and the signed-in user (`by`) generating them. */
public record GeneratePeriodsRequest(String from, String to, String by) {
}
