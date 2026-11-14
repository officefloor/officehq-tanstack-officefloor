package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the proof just added to the claim, with its person's name, then the claim's proofs count, then the kind chosen (none: left out). */
public class AuditClaimProofAdded {
    public void audit(@Val Claim claim, @Val ProofKind kind, PersonRepository people, @RequestParam("user") String user, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_PROOF_ADDED sheet=" + claim.sheetId() + " by=" + user + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().setScale(2).toPlainString()
                + " date=" + claim.date() + Claim.auditNumber(claim.number()) + " proofs=" + claim.proofs()
                + (kind.value() == null ? "" : " kind=" + kind.value()));
    }
}
