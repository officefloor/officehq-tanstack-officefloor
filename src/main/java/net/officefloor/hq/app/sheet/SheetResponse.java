package net.officefloor.hq.app.sheet;

/** A sheet as the screen reads it: its id, today, whether it is archived and its parent sheet id ("": none). */
public record SheetResponse(String id, String today, boolean archived, String parent) {

    public static SheetResponse from(Sheet sheet, String parent) {
        return new SheetResponse(sheet.id(), sheet.today().toString(), sheet.archived(), parent == null ? "" : parent);
    }
}
