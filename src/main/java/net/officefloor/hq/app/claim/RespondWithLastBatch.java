package net.officefloor.hq.app.claim;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The last bulk settle batch given on the sheet; 0 when none was. */
public class RespondWithLastBatch {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimStore store, ObjectResponse<Long> response) {
        response.send(store.lastBatch(sheetId.trim()));
    }
}
