package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The number of the sheet's last bulk settle batch (as of a date: the last made by then; see AsOf). */
public class RespondWithSettleBatch {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, ClaimRepository claims,
            ObjectResponse<SettleBatchResponse> response) {
        LocalDate date = AsOf.parse(asOf);
        int last = claims.lastBatch(sheetId.trim(), date);
        response.send(new SettleBatchResponse(last == 0 ? null : last));
    }
}
