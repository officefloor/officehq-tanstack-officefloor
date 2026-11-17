package net.officefloor.hq.app.period;

/** The validated year of generating quarterly periods, and who generates them. */
public record GenerateQuartersValues(int year, String by) {
}
