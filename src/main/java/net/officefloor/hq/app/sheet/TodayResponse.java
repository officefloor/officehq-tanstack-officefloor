package net.officefloor.hq.app.sheet;

/** The sheet's today and whether it is archived. */
public record TodayResponse(String sheet, String today, boolean archived) {

    public static TodayResponse of(String sheetId, Sheets sheets) {
        return new TodayResponse(sheetId, sheets.today(sheetId).toString(), sheets.archived(sheetId));
    }
}
