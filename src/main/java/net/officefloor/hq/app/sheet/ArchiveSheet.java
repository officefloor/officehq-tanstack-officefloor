package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward archives the sheet, and only once; anyone else, or a sheet already archived, is refused. */
public class ArchiveSheet {
    public void archive(@PathVariable("sheetId") String sheetId, @RequestBody ArchiveRequest request, Sheets sheets,
            Settings settings, Audit audit, ObjectResponse<TodayResponse> response) throws ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new ViewerRefusedException("Only a steward can archive the sheet");
        }
        sheets.archive(sheetId);
        audit.recordOnCommit("SHEET_ARCHIVED sheet=" + sheetId + " by=" + request.user().trim());
        response.send(TodayResponse.of(sheetId, sheets));
    }
}
