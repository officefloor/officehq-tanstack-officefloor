package net.officefloor.hq.app.columns;

/** One column switched on or off, as the screen sends it. */
public record ColumnRequest(String column, Boolean shown) {
}
