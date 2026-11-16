package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward sets the due-by of a DRAFT, OPEN or PENDING claim directly ({@code date}, yyyy-MM-dd). It
 *  stays as set (a holiday added later does not move it); its overdue, late charge, grace and period
 *  follow it. Refused for anyone else, for any other status, for a date that is not a date, and when
 *  the due-by it has or the one it would have falls in a closed period. */
public class SetClaimDueBy {
    public void set(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "date", required = false) String date, @Val Claim claim,
            RoleRepository roles, PeriodRepository periods, Out<DueBySetting> setting)
            throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
        String sheetId = claim.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can set a claim's due-by");
        }
        String status = claim.getStatus();
        if (!ClaimStatus.DRAFT.name().equals(status) && !ClaimStatus.isOpen(status)) {
            throw new ClaimStatusException("A " + status + " claim's due-by cannot be set");
        }
        LocalDate to;
        try {
            to = LocalDate.parse(date == null ? "" : date.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Not a date: " + date);
        }
        periods.requireOpen(sheetId, claim.getDueBy());
        periods.requireOpen(sheetId, to.toString());
        String old = claim.getDueBy();
        claim.setDueBy(to.toString());
        setting.set(new DueBySetting(old, claim.getDueBy()));
    }
}
