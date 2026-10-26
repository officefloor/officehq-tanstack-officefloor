package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** This sheet's record that the transfer which brought the person here was undone; its rollup lines follow. */
public class RecordPersonTransferUndone {
    public void record(@Val Person person, @Val PersonTransferred transferred, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + person.getSheetId() + " by=" + by.name() + " name="
                + transferred.name() + " to=" + transferred.to());
    }
}
