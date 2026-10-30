package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class AuditClaimProof {
    public void audit(@Val ProofRequest request, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_PROOF_ADDED", request.user(), person, claim, claim.getNumber())
                + " proofs=" + claim.getProofs() + (request.kind() == null ? "" : " kind=" + request.kind()));
    }
}
