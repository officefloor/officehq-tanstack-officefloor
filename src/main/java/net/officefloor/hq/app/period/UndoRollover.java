package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Removes the periods the rollover generated and takes back the opening balances it set, then writes
 *  ROLLOVER_UNDONE. */
public class UndoRollover {
    public void undo(@Val RolloverRepository.Rollover rollover, RolloverRepository rollovers, Audit audit)
            throws InvalidPeriodException {
        if (!rollovers.undo(rollover)) {
            throw new InvalidPeriodException("There is no rollover to undo");
        }
        audit.record("ROLLOVER_UNDONE sheet=" + rollover.sheetId() + " by=" + rollover.by() + " fy=" + rollover.fy());
    }
}
