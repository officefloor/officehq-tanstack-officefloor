package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Undoes the sheet's last rollover: the periods it generated are removed and the opening balances it
 * added are taken back; ROLLOVER_UNDONE with its old year. Only the steward who made it may, on the
 * same sheet-today it was made. Refused when there is no rollover, it is already undone, or a claim
 * (in any status, voided too) belongs to a period it generated, by its date or its due-by.
 */
public class UndoRollover {
    public void undo(@Val Sheet sheet, PeriodRepository periods, RolloverRepository rollovers, ClaimRepository claims,
            RoleRepository roles, HttpServletRequest request, Audit audit, Out<GeneratePeriodsResponse> result)
            throws StewardOnlyException, InvalidPeriodException {
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new StewardOnlyException("Only a steward may undo a rollover");
        }
        Rollover last = rollovers.last(sheet.id());
        if (last == null || last.undone()) throw new InvalidPeriodException("There is no rollover to undo");
        if (!last.madeBy().equals(user)) {
            throw new InvalidPeriodException("Only " + last.madeBy() + " may undo their rollover");
        }
        if (!last.madeOn().equals(sheet.today())) {
            throw new InvalidPeriodException("A rollover is undone only on the day it was made");
        }
        List<Period> generated = periods.findAll(sheet.id()).stream()
                .filter((p) -> last.periods().contains(p.name())).toList();
        for (Claim claim : claims.findAll(sheet.id())) {
            for (Period p : generated) {
                if ((claim.getDate() != null && p.contains(claim.getDate()))
                        || (claim.getDueBy() != null && p.contains(claim.getDueBy()))) {
                    throw new InvalidPeriodException("A claim belongs to period " + p.name());
                }
            }
        }
        if (!rollovers.markUndone(last)) throw new InvalidPeriodException("The rollover is already undone");
        for (Rollover.Opening o : last.openings()) {
            periods.subtractOpening(sheet.id(), o.periodName(), o.personId(), o.amount());
        }
        for (Period p : generated) periods.delete(sheet.id(), p.name());
        audit.record("ROLLOVER_UNDONE sheet=" + sheet.id() + " by=" + user + " fy=" + last.fy());
        result.set(new GeneratePeriodsResponse(generated.size()));
    }
}
