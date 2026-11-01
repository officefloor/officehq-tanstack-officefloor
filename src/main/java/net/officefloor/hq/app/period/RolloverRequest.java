package net.officefloor.hq.app.period;

/** Body of a year end rollover: the fiscal year (yyyy) being rolled over, and who rolls it. */
public record RolloverRequest(String year, String user) {
}
