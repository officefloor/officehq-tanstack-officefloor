package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the delegation is saved, so a refused one writes no audit line. */
public class RecordDelegated {
    public void record(@Val Delegation delegation, Audit audit) {
        audit.record("DELEGATED sheet=" + delegation.sheetId() + " by=" + delegation.by()
                + " user=" + delegation.name() + " from=" + delegation.from() + " to=" + delegation.to());
    }
}
