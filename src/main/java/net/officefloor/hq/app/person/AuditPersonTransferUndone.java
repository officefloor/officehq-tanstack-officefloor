package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** After the transfer back's own lines, records the undo on this sheet: PERSON_TRANSFER_UNDONE sheet=<this>
 *  by=<user> name=<name> to=<the sheet they went back to>, followed by how it moved this sheet's figures. */
public class AuditPersonTransferUndone {
    public void audit(@Val TransferRequest request, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " to=" + request.to());
        rollups.record(person.getSheetId(), request.user(), before);
    }
}
