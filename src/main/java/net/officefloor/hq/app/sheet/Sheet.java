package net.officefloor.hq.app.sheet;

/** A sheet: its id and its own today (ISO yyyy-MM-dd), which stands in for the real date. */
public record Sheet(String id, String today) {
}
