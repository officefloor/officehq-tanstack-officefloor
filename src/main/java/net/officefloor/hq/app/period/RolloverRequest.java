package net.officefloor.hq.app.period;

/** Body of rolling a fiscal year over: the year (yyyy) of the old fiscal year, FY<yyyy>. */
public record RolloverRequest(String year) {
}
