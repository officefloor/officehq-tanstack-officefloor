package net.officefloor.hq.app.sheet;

/** A sheet, as the screen reads it. */
public record SheetResponse(String id) {
    public static SheetResponse from(Sheet sheet) {
        return new SheetResponse(sheet.id());
    }
}
