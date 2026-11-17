package net.officefloor.hq.app.period;

/** Body of rolling a fiscal year over: the old year (yyyy, the FY<yyyy> it is named), and who rolls it over. */
public record RolloverRequest(String year, String by) {
}
