package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The number of the sheet's last bulk settle batch. */
public class LoadSettleBatch {
    public void service(@PathVariable("sheetId") String sheetId, Sheets sheets,
            ObjectResponse<SettleBatchResponse> response) {
        int last = sheets.lastSettleBatch(sheetId);
        response.send(new SettleBatchResponse(last == 0 ? "" : String.valueOf(last)));
    }
}
