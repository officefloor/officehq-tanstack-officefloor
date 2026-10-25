package net.officefloor.hq.app.period;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Soft-closes the period, fixing each person's due-total and carried figure for it, each pool member's cut for it, and its own due-total exactly as closing does, while claims
 *  whose due-by falls in it can still be added, edited and change status. Refuses a period already closed or soft-closed, and a
 *  soft close by a role the sheet's permission matrix does not allow to close. */
public class SoftClosePeriod {
    public void close(@Val Period period, PeriodRepository repository, ClaimRepository claims, PersonRepository people,
            RateRepository rates, SheetRepository sheets, PersonGradeRepository grades, ClaimLogRepository log,
            RoleRepository roles, AsOf asOf, @RequestHeader(name = "X-User", required = false) String user) throws InvalidPeriodException {
        Sheet sheet = sheets.findById(period.getSheetId()).orElseGet(() -> new Sheet(period.getSheetId()));
        String role = roles.roleOf(period.getSheetId(), AuditUser.decode(user));
        if (!sheet.allows("close", role)) {
            throw new InvalidPeriodException("A " + role + " cannot close a period.");
        }
        if (period.isClosed() || period.isSoftClosed()) {
            throw new InvalidPeriodException("The period is already closed.");
        }
        period.softClose(ClosePeriod.totals(period, sheet, claims, people, rates, grades, log),
                ClosePeriod.carried(period, repository, sheet, claims, people, rates, grades, log));
        period.fixPoolCuts(PoolCuts.live(sheet, asOf));
        repository.saveAndFlush(period);
    }
}
