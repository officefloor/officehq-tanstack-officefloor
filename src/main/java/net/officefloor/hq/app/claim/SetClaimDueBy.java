package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Sets a DRAFT, OPEN or PENDING claim's due-by directly (a steward). It stays as set: a holiday added
 * later does not move it. Overdue, the late charge, grace and its period follow it. A new due-by
 * inside a closed period is refused. A refusal writes nothing.
 */
public class SetClaimDueBy {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val DueBy dueBy, RoleRepository roles,
            PeriodRepository periods, ClaimRepository repository, HttpServletRequest request,
            Out<DueBySetting> settingOut) throws StewardOnlyException, InvalidClaimStatusException, ClosedPeriodException {
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new StewardOnlyException("Only a steward may set a claim's due-by");
        }
        ClaimStatus status = claim.getStatus();
        if (status != ClaimStatus.DRAFT && status != ClaimStatus.OPEN && status != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot have its due-by set");
        }
        Period closed = periods.closedOn(sheet.id(), dueBy.date());
        if (closed != null) throw new ClosedPeriodException(closed.name());

        LocalDate from = claim.getDueBy();
        claim.setDueBy(dueBy.date());
        claim.setDueBySet(true);
        repository.saveSetDueBy(claim);
        settingOut.set(new DueBySetting(from, dueBy.date()));
    }
}
