package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;

/** The other sheet's lines of a transfer: the person arrived, then the figures it moved there. */
public class RecordPersonArrived {
    public void record(@Val Transfer transfer, Audit audit, Rollup rollup) {
        audit.record("PERSON_TRANSFERRED sheet=" + transfer.to() + " by=" + transfer.user()
                + " name=" + transfer.name() + " from=" + transfer.from());
        if (transfer.into() != null) {
            audit.record("PERSON_MERGED sheet=" + transfer.to() + " by=" + transfer.user()
                    + " from=" + transfer.name() + " into=" + transfer.into().getName());
        }
        rollup.record(transfer.to(), transfer.toBefore(), transfer.user());
    }
}
