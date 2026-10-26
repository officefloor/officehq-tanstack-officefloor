package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** This sheet's record of the transfer; its rollup lines follow. */
public class RecordPersonTransferred {
    public void record(@Val Person person, @Val PersonTransferred transferred, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_TRANSFERRED sheet=" + person.getSheetId() + " by=" + by.name() + " name="
                + transferred.name() + " to=" + transferred.to());
    }
}
