package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** Hold an OPEN or PENDING claim, or release it when it is held: a held claim still counts in every total (a hold
 *  acts only on pool cuts, and only a person's). A claim in any other status is
 *  refused. Every check is made before anything is written, so a refusal writes nothing to the store or the audit. */
final class ClaimHold {
    private ClaimHold() {
    }

    static Claim toggle(String id, long cid, String by, ClaimRepository repository, SettingsRepository settings,
            Audit audit, Rollup rollup) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.open()) {
            throw new BadRequestException("a " + found.status() + " claim cannot be held or released");
        }
        boolean hold = !found.held();
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.hold(id, cid, hold)) {
            throw new BadRequestException("the claim cannot be " + (hold ? "held" : "released"));
        }
        audit.record((hold ? "CLAIM_HELD" : "CLAIM_RELEASED") + " sheet=" + id + " by=" + by + " person="
                + repository.personName(id, found.personId()) + " amount=" + found.amount() + " date=" + found.date()
                + Claim.auditNumber(found.number()));
        rollup.record(id, by, before);
        return repository.find(id, cid);
    }
}
