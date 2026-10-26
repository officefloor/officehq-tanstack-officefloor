package net.officefloor.hq.app.sheet;

/** The sheet as the screen sees it. */
public record SheetResponse(String id) {

    public static SheetResponse from(Sheet sheet) {
        return new SheetResponse(sheet.id());
    }
}
