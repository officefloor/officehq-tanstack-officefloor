package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;

/** Remind an OPEN or PENDING claim: its reminded date becomes the sheet's today, and REMIND is written. A claim can be
 *  reminded once per sheet-today; a second reminder that day, or one of a claim in any other status, is refused and
 *  writes nothing to the store or the audit. */
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
        audit.record("REMIND sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number()));
        return repository.find(id, cid);
    }
}
