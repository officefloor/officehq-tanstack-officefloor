package net.officefloor.hq.app.rate;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Withdraws the newest version of a grade's rate from a from-date: the version before it, when there
 * is one, applies again; with none the rate is gone. Only a steward may; a rate the sheet itself does
 * not have is refused.
 */
public class WithdrawRate {
    public void withdraw(@Val WithdrawnRate rate, @Val Sheet sheet, RateRepository repository, RoleRepository roles,
            HttpServletRequest request) throws StewardOnlyException, InvalidRateException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may withdraw a rate");
        }
        if (!repository.withdraw(sheet.id(), rate.grade(), rate.from())) {
            throw new InvalidRateException("Grade " + rate.grade() + " has no rate"
                    + (rate.from() == null ? "" : " from " + rate.from()) + " to withdraw");
        }
    }
}
