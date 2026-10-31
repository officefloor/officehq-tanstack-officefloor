package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the transfer is saved, so a refused one writes no audit line: this sheet's line. */
public class RecordPersonTransferred {
    public void record(@Val Transfer transfer, Audit audit) {
        audit.record("PERSON_TRANSFERRED sheet=" + transfer.from() + " by=" + transfer.user()
                + " name=" + transfer.name() + " to=" + transfer.to());
    }
}
