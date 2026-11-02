package net.officefloor.hq.app.sheet;

/** A sheet: its id, its own today (ISO yyyy-MM-dd), which stands in for the real date, and whether a steward
 *  has archived it (nothing on it can then be changed). */
public record Sheet(String id, String today, boolean archived) {
}
