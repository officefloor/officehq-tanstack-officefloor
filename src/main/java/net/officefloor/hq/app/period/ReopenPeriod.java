package net.officefloor.hq.app.period;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Reopens a closed period, so its due-total is live again. Only a role the sheet's permission matrix allows to
 *  reopen may (a steward, by the standing rule); refuses a period
 *  that is not closed. */
public class ReopenPeriod {
    public void reopen(@Val Period period, @RequestHeader(name = "X-User", required = false) String user,
            @Val Sheet sheet, PeriodRepository repository, RoleRepository roles) throws InvalidPeriodException {
        String role = roles.roleOf(period.getSheetId(), AuditUser.decode(user));
        if (!sheet.allows("reopen", role)) {
            throw new InvalidPeriodException("A " + role + " cannot reopen a period.");
        }
        if (!period.isClosed()) {
            throw new InvalidPeriodException("The period is not closed.");
        }
        period.reopen();
        repository.saveAndFlush(period);
    }
}
