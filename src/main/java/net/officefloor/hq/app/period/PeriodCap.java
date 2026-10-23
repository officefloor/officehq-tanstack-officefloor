package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;

/** A period's cap on each person's OPEN or PENDING claims in it (a claim is in the period holding its due-by). */
public final class PeriodCap {

    private PeriodCap() {
    }

    /** The capped period the claim belongs to when its person already has as many OPEN or PENDING claims in it (other
     *  than this one) as its cap, so opening the claim would pass the cap; else null. */
    public static Period reached(Claim claim, PeriodRepository periods, ClaimRepository claims) {
        Period period = periods.on(claim.sheetId(), claim.dueBy());
        if (period == null || period.cap() == null) {
            return null;
        }
        long open = claims.listForPerson(claim.sheetId(), claim.personId()).stream()
                .filter(c -> c.id() != claim.id() && c.status().unsettled() && period.contains(c.dueBy())).count();
        return open >= period.cap() ? period : null;
    }
}
