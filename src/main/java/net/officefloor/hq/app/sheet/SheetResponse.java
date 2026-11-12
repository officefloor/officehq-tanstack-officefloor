package net.officefloor.hq.app.sheet;

/** A sheet as the front end reads it. {@code today} is ISO yyyy-MM-dd; an archived sheet
 *  refuses every change. {@code asOf} (yyyy-MM-dd) is the date the screen shows the sheet as of, null when
 *  it shows it as it is now; {@code archived} is then whether it was archived by the end of that date. */
public record SheetResponse(String id, String today, boolean archived, String asOf) {
}
