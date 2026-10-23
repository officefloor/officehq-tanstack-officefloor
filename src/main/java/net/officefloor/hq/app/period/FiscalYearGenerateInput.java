package net.officefloor.hq.app.period;

/** A validated fiscal year generate: who is making it, and the calendar year the fiscal year's first day falls in. */
public record FiscalYearGenerateInput(String user, int year) {
}
