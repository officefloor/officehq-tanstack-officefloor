package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.period.RolloverStore.Rollover;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's last rollover to undo. Refuses when there is none or it is already undone, when another user
 *  made it, when it was made on a sheet-today other than this one, or when a claim (not deleted) is dated in a
 *  period it generated. */
public class LoadRolloverUndo {
    public void load(@PathVariable("sheetId") String sheetId, @Val RolloverUndoRequest request,
            RolloverStore rollovers, PeriodStore periods, SheetStore sheets, Out<Rollover> out)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        Rollover rollover = rollovers.last(sheet);
        if (rollover == null || rollover.undone()) {
            throw new InvalidSheetException("There is no rollover to undo");
        }
        if (!rollover.madeBy().equals(request.user())) {
            throw new InvalidSheetException("Only " + rollover.madeBy() + " may undo their rollover");
        }
        if (!rollover.madeOn().equals(sheets.today(sheet))) {
            throw new InvalidSheetException("A rollover can be undone only on the day it was made");
        }
        for (String name : rollovers.periods(rollover.id())) {
            PeriodResponse period = periods.find(sheet, name);
            if (period != null && rollovers.claimIn(sheet, LocalDate.parse(period.from()), LocalDate.parse(period.to()))) {
                throw new InvalidSheetException("A claim belongs to period " + name);
            }
        }
        out.set(rollover);
    }
}
