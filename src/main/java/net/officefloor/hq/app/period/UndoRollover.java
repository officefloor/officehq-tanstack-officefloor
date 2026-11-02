package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Undoes the sheet's last year end rollover: takes back the opening balances it set and removes the periods it
 * generated. Only the steward who made it may, on the same sheet-today it was made; refuses when there is none to
 * undo, or when a claim (of any status) is dated in a period it generated.
 */
public class UndoRollover {
    public void undo(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Periods periods,
            Rollovers rollovers, Claims claims, Roles roles, SheetClock clock, Audit audit)
            throws InvalidPeriodException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPeriodException("Only a steward may undo a rollover");
        }
        Rollovers.Rollover last = rollovers.last(sheet);
        if (last == null) {
            throw new InvalidPeriodException("There is no rollover to undo");
        }
        if (!last.by().equals(user)) {
            throw new InvalidPeriodException("Only the steward who made the rollover may undo it");
        }
        if (!last.on().equals(clock.today(sheet))) {
            throw new InvalidPeriodException("A rollover can only be undone on the day it was made");
        }
        List<Period> generated = periods.list(sheet).stream().filter((p) -> last.periods().contains(p.name()))
                .toList();
        for (Claim claim : claims.list(sheet)) {
            LocalDate date = LocalDate.parse(claim.date());
            for (Period period : generated) {
                if (!date.isBefore(LocalDate.parse(period.from())) && !date.isAfter(LocalDate.parse(period.to()))) {
                    throw new InvalidPeriodException("A claim belongs to the generated period " + period.name());
                }
            }
        }
        last.openings().forEach((o) -> periods.takeOpening(sheet, o.period(), o.person(), o.amount()));
        generated.forEach((p) -> periods.remove(sheet, p.name()));
        rollovers.forget(sheet);
        audit.record("ROLLOVER_UNDONE sheet=" + sheet + " by=" + user + " fy=" + last.fy());
    }
}
