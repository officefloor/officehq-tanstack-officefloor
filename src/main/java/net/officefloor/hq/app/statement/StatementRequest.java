package net.officefloor.hq.app.statement;

/** Body of POST /api/sheets/{sheet}/people/{person}/statement; a blank (or absent) as-of date takes the statement now. */
public record StatementRequest(String asof) {
}
