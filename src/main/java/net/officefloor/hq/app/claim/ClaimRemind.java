package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;

/** Remind an OPEN or PENDING claim: its reminded date becomes the sheet's today, and REMIND is written. A claim can be
 *  reminded once per sheet-today; a second reminder that day, or one of a claim in any other status, is refused and
 *  writes nothing to the store or the audit. A reminder whose previous one was at least the sheet's escalation gap
 *  before today escalates the claim (once per claim ever): ESCALATE is written after REMIND. */
final class ClaimRemind {
    private ClaimRemind() {
    }

    static Claim apply(String id, long cid, String by, ClaimRepository repository, Audit audit) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.open()) {
            throw new BadRequestException("a " + found.status() + " claim cannot be reminded");
        }
        if (!repository.remind(id, cid)) {
            throw new BadRequestException("the claim was already reminded today");
        }
        String named = " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number());
        audit.record("REMIND sheet=" + id + " by=" + by + named);
        if (repository.escalate(id, cid, found.reminded())) {
            audit.record("ESCALATE sheet=" + id + " by=" + by + named);
        }
        return repository.find(id, cid);
    }
}
