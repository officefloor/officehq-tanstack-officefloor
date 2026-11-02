package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheet}/periods/rollover: the fiscal year to roll over, by the calendar year its first day falls in. */
public record RolloverRequest(String year) {
}
