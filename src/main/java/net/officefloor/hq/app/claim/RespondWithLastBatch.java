package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the sheet's last settle batch number (null: none yet). */
public class RespondWithLastBatch {

    /** The sheet's last settle batch number, or null when it has had none. */
    public record LastBatch(Integer last) {
    }

    public void respond(@Val Sheet sheet, SettleBatchRepository batches, ObjectResponse<LastBatch> response) {
        response.send(new LastBatch(batches.last(sheet.id())));
    }
}
