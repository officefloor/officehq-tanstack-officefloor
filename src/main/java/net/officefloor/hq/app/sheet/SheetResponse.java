package net.officefloor.hq.app.sheet;

/** A sheet as the front end reads it. {@code today} is ISO yyyy-MM-dd; an archived sheet
 *  refuses every change. */
public record SheetResponse(String id, String today, boolean archived) {
}
