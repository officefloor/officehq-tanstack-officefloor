package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: a proof that was added, ending with the claim's proofs count, then its kind when one
 *  was chosen. */
public class RecordClaimProofAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "kind", required = false) String kind,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_PROOF_ADDED", by, claim, people)
                + " proofs=" + claim.getProofs()
                + (kind == null || kind.isBlank() ? "" : " kind=" + kind.trim()));
    }
}
