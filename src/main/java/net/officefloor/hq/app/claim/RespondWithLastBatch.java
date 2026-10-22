package net.officefloor.hq.app.claim;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The number of the sheet's last settle batch (null when there has been none). */
public class RespondWithLastBatch {
    public void respond(@PathVariable("sheetId") String sheetId, SettleBatchRepository batches,
            ObjectResponse<LastBatchResponse> response) {
        response.send(new LastBatchResponse(batches.last(sheetId.trim())));
    }
}
