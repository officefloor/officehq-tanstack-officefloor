package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.RolloverStore.Rollover;
import net.officefloor.hq.app.period.RolloverStore.RolloverOpening;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Undoes the rollover: removes the periods it generated, takes back the opening balances it set, and records
 *  ROLLOVER_UNDONE sheet=<id> by=<user> fy=FY<old year>. */
public class UndoRollover {
    public void undo(@PathVariable("sheetId") String sheetId, @Val RolloverUndoRequest request,
            @Val Rollover rollover, RolloverStore rollovers, Audit audit, Out<RolloverUndoResponse> response)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        if (!rollovers.markUndone(rollover.id())) {
            throw new InvalidSheetException("There is no rollover to undo");
        }
        for (RolloverOpening opening : rollovers.openings(rollover.id())) {
            rollovers.takeOpeningBack(sheet, opening);
        }
        int removed = 0;
        for (String name : rollovers.periods(rollover.id())) {
            rollovers.removePeriod(sheet, name);
            removed++;
        }
        audit.record("ROLLOVER_UNDONE sheet=" + sheet + " by=" + request.user() + " fy=FY" + rollover.fy());
        response.set(new RolloverUndoResponse("FY" + rollover.fy(), removed));
    }
}
