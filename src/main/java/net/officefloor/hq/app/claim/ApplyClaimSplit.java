package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;

/** Splits an amount off an OPEN claim (not deferred, nor dated in a closed period), for a user whose
 *  role may open a claim, and less than its amount: the claim keeps the rest, unbound from any
 *  statement; the new OPEN claim has the amount split off, the same date and note, the next number,
 *  is opened by the user, its due-by fixed from the terms in force on its date, and names the
 *  original's number as the one it was split from. */
public class ApplyClaimSplit {
    public void apply(@Val ClaimSplit split, @Val Claim claim, ClaimNumbers numbers, People people,
            ClosedPeriods closedPeriods, Settings settings) throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        if (claim.getStatus() != ClaimStatus.OPEN || claim.isDeferred()) {
            throw new ClaimStatusRefusedException("Only an OPEN claim that is not deferred can be split");
        }
        if (!settings.allows(claim.getSheetId(), "open", split.user())) {
            throw new ClaimStatusRefusedException("Your role may not open a claim, so may not split one");
        }
        BigDecimal rest = claim.getAmount().subtract(split.amount());
        if (rest.signum() <= 0) {
            throw new ClaimStatusRefusedException("A split amount must be less than the claim's amount");
        }
        claim.setAmount(rest);
        claim.setStated(false);
        claim.setStatedDue(null);
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Claim into = new Claim(null, claim.getSheetId(), claim.getPersonId(), split.amount(), claim.getDate(),
                claim.getNote(), ClaimStatus.OPEN, null, null, split.user());
        into.setDueBy(BuildClaim.dueBy(person, claim.getDate(), settings));
        into.setSplitFrom(claim.getNumber());
        into.setNumber(numbers.next(into));
        split.into(into);
    }
}
