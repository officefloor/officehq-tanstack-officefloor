package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;

/** A claim added while its period (by its due-by) is soft-closed is a late entry: marked so, and audited as LATE_ENTRY after its CLAIM_ADDED. */
final class LateEntry {

    private LateEntry() {
    }

    static void mark(Claim claim, Person person, String user, Claims claims, Periods periods, Audit audit) {
        if (periods.isSoftClosed(claim.getSheetId(), claim.getPeriodDate())) {
            claims.markLateEntry(claim);
            audit.recordOnCommit(ClaimAudit.line("LATE_ENTRY", user, person, claim, claim.getNumber()));
        }
    }
}
