package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored proof: CLAIM_PROOF_ADDED sheet=<id> by=<user> person=<name> amount=<0.00>
 *  date=<yyyy-mm-dd>, then number=<n> when the claim has one, then proofs=<count>, then kind=<kind> when a kind was chosen. */
public class AuditClaimProof {
    public void audit(@Val ProofInput input, @Val Claim claim, PersonStore people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_PROOF_ADDED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber())
                + " proofs=" + claim.getProofs()
                + (input.kind() == null ? "" : " kind=" + input.kind()));
    }
}
