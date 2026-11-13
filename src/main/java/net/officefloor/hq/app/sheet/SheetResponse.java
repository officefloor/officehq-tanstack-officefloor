package net.officefloor.hq.app.sheet;

public record SheetResponse(String id, String today, boolean archived) {

    public static SheetResponse from(Sheet sheet) {
        return new SheetResponse(sheet.id(), sheet.today().toString(), sheet.archived());
    }
}
