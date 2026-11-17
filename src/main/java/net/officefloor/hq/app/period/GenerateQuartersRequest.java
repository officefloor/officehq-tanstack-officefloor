package net.officefloor.hq.app.period;

/** Body of generating a year's quarterly periods: the year (yyyy), and who generates. */
public record GenerateQuartersRequest(String year, String by) {
}
