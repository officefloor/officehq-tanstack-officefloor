package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A steward waives an OPEN or PENDING claim's late charge: from then on it bears none, whatever its overdue state.
 *  Anyone else, a claim in any other status, or one already waived is refused. Every check is made before anything
 *  is written, so a refusal writes nothing to the store or the audit. */
final class ClaimWaive {
    private ClaimWaive() {
    }

    static Claim apply(String id, long cid, String by, ClaimRepository repository, SettingsRepository settings,
            Audit audit, Rollup rollup) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can waive a claim's late charge");
        }
        if (!found.open()) {
            throw new BadRequestException("a " + found.status() + " claim's late charge cannot be waived");
        }
        if (found.waived()) {
            throw new BadRequestException("the claim's late charge is already waived");
        }
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.waive(id, cid)) {
            throw new BadRequestException("the claim's late charge cannot be waived");
        }
        audit.record("CLAIM_WAIVED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number()));
        rollup.record(id, by, before);
        return repository.find(id, cid);
    }
}
