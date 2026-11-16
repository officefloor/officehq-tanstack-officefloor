package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Edits a claim by the claim form's rules ({@link ClaimEdit}). */
public class ApplyClaim {
    public void apply(@RequestParam(name = "by", required = false) String by, @Val ValidClaim request,
            @Val Claim claim, SheetRepository sheets,
            PeriodRepository periods, PersonRepository people, ClaimRepository claims, ClaimLineRepository lines)
            throws ClaimStatusException, PeriodClosedException, InvalidClaimException, ClaimLockedException {
        ClaimEdit.apply(by, request, claim, sheets, periods, people, claims, lines);
    }
}
