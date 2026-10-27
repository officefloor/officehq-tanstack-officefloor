package net.officefloor.hq.app.batch;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettleBatch {
    public void respond(@PathVariable("sheetId") String sheetId, SettleBatches batches,
            ObjectResponse<SettleBatchResponse> response) {
        response.send(new SettleBatchResponse(batches.last(sheetId.trim())));
    }
}
