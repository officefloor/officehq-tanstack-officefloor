package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "preview", required = false) String preview, Settings settings, Sheets sheets,
            AsOf asOfSheet, ObjectResponse<SettingsResponse> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, preview, () -> SettingsResponse.of(sheetId, sheets.today(sheetId), settings)));
    }
}
