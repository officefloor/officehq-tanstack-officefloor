package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sets a DRAFT, OPEN or PENDING claim's due-by directly; only a steward may. A claim in any other status, or a due-by
 * (old or new) in a closed period, is refused. The due-by stays as set: a holiday added later does not move it, and
 * overdue, the late charge, grace and the claim's period follow it. Returns the old and new due-by, for the audit.
 */
public class ApplyDueBy {
    public DueBySet apply(@Val Claim claim, ClaimRepository repository, PeriodRepository periods, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader,
            @RequestParam(name = "date", required = false) String dateParam) throws ClaimStatusException {
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            throw new ClaimStatusException("Only a steward can set a claim's due-by.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateParam == null ? "" : dateParam.trim());
        } catch (DateTimeParseException e) {
            throw new ClaimStatusException("Not a date: " + dateParam);
        }
        ClaimStatus status = claim.getStatus();
        if (status != ClaimStatus.DRAFT && status != ClaimStatus.OPEN && status != ClaimStatus.PENDING) {
            throw new ClaimStatusException("A " + status + " claim cannot have its due-by set.");
        }
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy()) || periods.isClosedOn(claim.getSheetId(), date)) {
            throw new ClaimStatusException("A claim's due-by cannot be set from or into a closed period.");
        }
        LocalDate old = claim.getDueBy();
        claim.setDueBy(date);
        repository.saveAndFlush(claim);
        return new DueBySet(old, date);
    }
}
