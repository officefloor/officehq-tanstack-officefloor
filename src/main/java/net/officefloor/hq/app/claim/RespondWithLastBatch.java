package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The number of the sheet's last settle batch (null when there has been none); given an as-of date, the last made
 *  on or before it. */
public class RespondWithLastBatch {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SettleBatchRepository batches,
            ObjectResponse<LastBatchResponse> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        response.send(new LastBatchResponse(end != null ? batches.lastAsOf(id, end) : batches.last(id)));
    }
}
