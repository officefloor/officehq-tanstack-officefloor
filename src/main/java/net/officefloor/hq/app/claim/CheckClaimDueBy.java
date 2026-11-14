package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) setting a due-by unless a steward sets it on a DRAFT, OPEN or PENDING claim to a date not inside a
 * closed period (its period is the one containing its due-by); publishes the change.
 */
public class CheckClaimDueBy {
    public void check(@RequestParam("user") String user, @Val Claim claim, @Val NewDueByDate newDueBy,
            SettingRepository settings, PeriodRepository periods, Out<DueBySetting> setting)
            throws InvalidClaimException {
        LocalDate dueBy = newDueBy.value();
        if (settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can set a claim's due-by.");
        }
        if (claim.status() != ClaimStatus.DRAFT && !claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim's due-by cannot be set.");
        }
        Period closed = periods.closedOn(claim.sheetId(), dueBy);
        if (closed != null) {
            throw new InvalidClaimException(dueBy + " is in the closed period " + closed.name() + ".");
        }
        setting.set(new DueBySetting(claim.dueBy(), dueBy));
    }
}
