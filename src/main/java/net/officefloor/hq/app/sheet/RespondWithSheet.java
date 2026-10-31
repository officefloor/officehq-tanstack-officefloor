package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet; seen as of a date (?asOf=), as it stood then, with that date for its today. */
public class RespondWithSheet {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Sheets sheets, AsOf asOfSheet,
            ObjectResponse<SheetResponse> response) throws InvalidSheetRequestException {
        sheets.today(sheetId);
        response.send(asOfSheet.view(sheetId, asOf,
                () -> new SheetResponse(sheetId, sheets.today(sheetId).toString(), sheets.archived(sheetId))));
    }
}
