package net.officefloor.hq.app.period;

/** Body of a quarters generate: the calendar year (yyyy) whose four quarters get a period each, and
 *  who generates them. */
public record GenerateQuartersRequest(String year, String user) {
}
