package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Reviews a REVIEW claim: it becomes OPEN (still unnumbered until settled, see {@link ClaimNumbers}); who opened it
 *  stays as it was. Refused unless the user is a steward, when it is not REVIEW, for one due by a day
 *  in a closed period, and when its period caps its person's open claims and they already have that many. */
public class ReviewClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            ClaimRepository claims, PeriodRepository periods, RoleRepository roles,
            SettingRepository settings, SheetRepository sheets)
            throws ClaimStatusException, PeriodClosedException {
        if (!ClaimStatus.REVIEW.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be reviewed");
        }
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can review a claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        OpenClaim.requireUnderCap(claim, claims, periods);
        claim.setStatus(ClaimStatus.REVIEW.moveTo(ClaimStatus.OPEN).name());
    }
}
