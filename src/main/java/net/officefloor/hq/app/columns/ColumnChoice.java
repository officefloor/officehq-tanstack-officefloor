package net.officefloor.hq.app.columns;

/** {@code user} shows (or switches off) {@code column} of the people list. */
public record ColumnChoice(String user, String column, boolean shown) {
}
