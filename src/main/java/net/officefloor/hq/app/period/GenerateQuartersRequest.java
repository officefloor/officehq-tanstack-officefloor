package net.officefloor.hq.app.period;

/** Body of generating a year's calendar quarters: the year (yyyy) that names them, <yyyy>-Q1 to <yyyy>-Q4. */
public record GenerateQuartersRequest(String year) {
}
