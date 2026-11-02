package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheet}/periods/generate-quarters: the calendar year (yyyy) whose quarters are made. */
public record GenerateQuartersRequest(String year) {
}
