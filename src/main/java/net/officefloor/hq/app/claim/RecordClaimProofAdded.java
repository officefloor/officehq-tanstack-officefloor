package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_PROOF_ADDED to the audit file once the proof is stored, naming the claim (number=<number> when it has
 *  one) and ending with proofs=<count>. */
public class RecordClaimProofAdded {
    public void record(@Val Claim claim, @Val ClaimProof proof, PersonRepository people, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_PROOF_ADDED sheet=" + claim.sheetId() + " by=" + proof.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number())
                + " proofs=" + claim.proofs());
    }
}
