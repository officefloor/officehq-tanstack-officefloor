package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * A period's cap on open claims: a claim belongs to the period containing its due-by, and while
 * that period has a cap, its person may have at most that many OPEN or PENDING claims in it.
 */
public final class PeriodCaps {
    private PeriodCaps() {
    }

    /** The capped period the claim belongs to when its person is already at the cap, else null. */
    public static Period full(Sheet sheet, Claim claim, PeriodRepository periods, ClaimRepository claims) {
        if (claim.getDueBy() == null) return null;
        Period period = periods.on(sheet.id(), claim.getDueBy());
        if (period == null || period.cap() == null) return null;
        long held = claims.findByPerson(sheet.id(), claim.getPersonId()).stream()
                .filter(c -> !c.getId().equals(claim.getId()))
                .filter(c -> c.getStatus() == ClaimStatus.OPEN || c.getStatus() == ClaimStatus.PENDING)
                .filter(c -> c.getDueBy() != null && period.contains(c.getDueBy()))
                .count();
        return held >= period.cap() ? period : null;
    }
}
