package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body and loads the sheet's last rollover: there is one not yet undone, the one undoing it is the steward
 *  who made it, the sheet's today is the day it was made on, and no claim (whatever its status) is dated inside a
 *  period it generated, nor an opening balance pasted by hand on one. Publishes the rollover and who undoes it. */
public class ValidateRolloverUndo {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RolloverUndoRequest request,
            RoleRepository roles, SheetRepository sheets, PeriodRepository periods, RolloverRepository rollovers,
            Out<RolloverRepository.Rollover> rolloverOut) throws InvalidPeriodException {
        String sheet = sheetId.trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheet, by))) {
            throw new InvalidPeriodException("Only a steward may undo a rollover");
        }
        RolloverRepository.Rollover rollover = rollovers.last(sheet);
        if (rollover == null || rollover.undone()) {
            throw new InvalidPeriodException("There is no rollover to undo");
        }
        if (!rollover.by().equals(by)) {
            throw new InvalidPeriodException("Only the steward who made the rollover may undo it");
        }
        if (!sheets.today(sheet).equals(rollover.madeOn())) {
            throw new InvalidPeriodException("A rollover can only be undone on the day it was made");
        }
        for (long id : rollover.periodIds()) {
            Period period = periods.find(sheet, id);
            if (period == null) {
                continue;
            }
            if (rollovers.hasClaims(period)) {
                throw new InvalidPeriodException("A claim belongs to the period " + period.name());
            }
            if (rollovers.hasOtherOpenings(period, rollover.openingIds())) {
                throw new InvalidPeriodException("The period " + period.name() + " has opening balances pasted on it");
            }
        }
        rolloverOut.set(rollover);
    }
}
