package net.officefloor.hq.app.sheet;

public record SheetResponse(String id, String today) {

    public static SheetResponse from(Sheet sheet) {
        return new SheetResponse(sheet.id(), sheet.today().toString());
    }
}
