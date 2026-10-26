package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** CLAIM_PROOF_ADDED, naming the claim, then its proofs count now, then the kind when one was chosen. */
public class RecordClaimProofAdded {
    public void record(@Val Claim claim, @Val ChangedBy by, @Val ChosenProofKind kind, PersonRepository people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_PROOF_ADDED", claim, by, person == null ? "" : person.getName())
                + " proofs=" + claim.getProofs() + (kind.chosen() == null ? "" : " kind=" + kind.chosen()));
    }
}
