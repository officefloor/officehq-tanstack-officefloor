package net.officefloor.hq.app.columns;

/** The body of POST /api/sheets/{sheet}/columns: one column switched on (shown) or off. */
public record ColumnChoice(String column, Boolean shown) {
}
