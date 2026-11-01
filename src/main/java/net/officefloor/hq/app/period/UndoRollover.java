package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Takes back the opening balances the rollover set and removes the periods it generated, audited
 *  ROLLOVER_UNDONE; the sheet then has no rollover to undo. */
public class UndoRollover {
    public void undo(@PathVariable("sheetId") String sheetId, @Val Rollovers.Made made, Periods periods,
            Rollovers rollovers, Audit audit, Out<RolloverUndone> result) {
        made.openings().forEach((period, amounts) -> amounts.forEach(
                (personId, amount) -> periods.takeOpening(sheetId, period, personId, amount)));
        for (String period : made.periods()) periods.remove(sheetId, period);
        rollovers.forget(sheetId);
        audit.record("ROLLOVER_UNDONE sheet=" + sheetId + " by=" + made.user() + " fy=FY" + made.fy());
        result.set(new RolloverUndone(made.fy(), made.periods().size()));
    }
}
