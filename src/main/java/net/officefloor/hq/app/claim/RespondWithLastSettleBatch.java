package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The number of the sheet's last bulk settle batch; null when there has been none. */
public class RespondWithLastSettleBatch {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<LastSettleBatchResponse> response) {
        response.send(new LastSettleBatchResponse(
                history.view(sheet.id(), asOf.on(), jdbc -> new ClaimRepository(jdbc).lastSettleBatch(sheet.id()))));
    }
}
