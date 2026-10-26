package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** The other sheet's record of the transfer (and of the merge it made there), followed by that sheet's rollup lines. */
public class RecordPersonArrived {
    public void record(@Val Person person, @Val PersonTransferred transferred, @Val ChangedBy by, Audit audit,
            Rollup rollup) {
        audit.record("PERSON_TRANSFERRED sheet=" + transferred.to() + " by=" + by.name() + " name="
                + transferred.name() + " from=" + person.getSheetId());
        if (transferred.mergedInto() != null) {
            audit.record("PERSON_MERGED sheet=" + transferred.to() + " by=" + by.name() + " from=" + transferred.name()
                    + " into=" + transferred.mergedInto());
        }
        rollup.record(transferred.to(), transferred.toBefore(), by);
    }
}
