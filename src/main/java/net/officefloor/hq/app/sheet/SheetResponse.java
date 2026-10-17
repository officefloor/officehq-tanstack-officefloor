package net.officefloor.hq.app.sheet;

public record SheetResponse(String id) {

    public static SheetResponse from(Sheet sheet) {
        return new SheetResponse(sheet.getId());
    }
}
