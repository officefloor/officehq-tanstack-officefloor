package net.officefloor.hq.app.rollover;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Take back the opening balances the rollover set, remove the periods it generated, mark it undone and write
 *  ROLLOVER_UNDONE fy=<old FY>. */
public class UndoRollover {
    public void undo(@PathVariable("sheetId") String sheetId, @Val RolloverUndoInput input,
            @Val RolloverRepository.Rollover rollover, RolloverRepository rollovers, PeriodRepository periods,
            Audit audit, ObjectResponse<RolloverUndoResponse> response) {
        String id = sheetId.trim();
        periods.removeOpenings(id, rollover.openingIds());
        rollover.periodNames().forEach(name -> periods.remove(id, name));
        rollovers.markUndone(rollover.id());
        audit.record("ROLLOVER_UNDONE sheet=" + id + " by=" + input.user() + " fy=" + rollover.fy());
        response.send(new RolloverUndoResponse(rollover.fy(), rollover.periodNames().size()));
    }
}
