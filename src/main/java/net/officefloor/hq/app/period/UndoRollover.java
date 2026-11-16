package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Undoes the sheet's last rollover: removes the periods it generated and takes back the opening balances it
 *  set, recorded as ROLLOVER_UNDONE fy=FY<yyyy>; it is then no longer the last rollover. Refused, with nothing
 *  written, when there is none, for anyone but the steward who made it, on a sheet-today other than the one it
 *  was made on, or when a claim (whatever its status) belongs to a period it generated. */
public class UndoRollover {
    public void undo(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, SheetRepository sheets, PeriodRepository periods,
            ClaimRepository claims, RoleRepository roles, RolloverRepository rollovers, Audit audit,
            Out<GeneratePeriodsResult> result) throws InvalidPeriodException, PeriodPermissionException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        RolloverRepository.Last last = rollovers.last(id);
        if (last == null) {
            throw new InvalidPeriodException("There is no rollover to undo");
        }
        if (!Role.STEWARD.equals(roles.roleOf(id, user)) || !last.by().equals(user)) {
            throw new PeriodPermissionException("Only the steward who rolled the year over can undo it");
        }
        if (!last.on().equals(sheets.today(id))) {
            throw new InvalidPeriodException("A rollover can be undone only on the day it was made");
        }
        List<Period> made = last.periods().stream().map((name) -> periods.find(id, name))
                .filter((p) -> p != null).toList();
        for (Claim c : claims.list(id)) {
            if (made.stream().anyMatch((p) -> p.contains(c.getDueBy()))) {
                throw new InvalidPeriodException("A claim belongs to a period the rollover made");
            }
        }

        sheets.open(id, user);
        for (RolloverRepository.Opening o : rollovers.openings(id)) {
            periods.takeOpening(id, o.period(), o.personId(), o.amount(), o.created());
        }
        for (Period p : made) {
            periods.remove(id, p.name());
        }
        rollovers.forget(id);
        audit.record("ROLLOVER_UNDONE sheet=" + id + " by=" + user + " fy=" + last.fy());
        result.set(new GeneratePeriodsResult(made.size()));
    }
}
