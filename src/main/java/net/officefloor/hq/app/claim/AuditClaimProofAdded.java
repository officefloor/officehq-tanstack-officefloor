package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the proof, once it is saved: the claim's line, then its proofs count, then the kind when one was chosen. */
public class AuditClaimProofAdded {
    public void audit(@Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user,
            @RequestParam(name = "kind", required = false) String kind, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line("CLAIM_PROOF_ADDED", claim, person, AuditUser.decode(user)) + " proofs=" + claim.getProofs()
                + (kind == null || kind.isBlank() ? "" : " kind=" + kind.trim()));
    }
}
