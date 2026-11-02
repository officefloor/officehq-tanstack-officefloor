package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;

/** Raise a claim's proofs count by one: not a SETTLED or VOID claim, nor one in a closed period. Every check is
 *  made before anything is written, so a refusal writes nothing to the store or the audit. */
final class ClaimProof {
    private ClaimProof() {
    }

    static Claim apply(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods,
            Audit audit) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (found.status().equals("SETTLED") || found.status().equals("VOID")) {
            throw new BadRequestException("a " + found.status() + " claim cannot be given a proof");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        Integer proofs = repository.addProof(id, cid);
        if (proofs == null) {
            throw new BadRequestException("a " + found.status() + " claim cannot be given a proof");
        }
        audit.record("CLAIM_PROOF_ADDED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number())
                + " proofs=" + proofs);
        return repository.find(id, cid);
    }
}
