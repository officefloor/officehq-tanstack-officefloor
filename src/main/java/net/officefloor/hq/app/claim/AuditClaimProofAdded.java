package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored proof in the audit file, naming the claim like every claim line, then its proofs count now, then the kind when one was chosen. */
public class AuditClaimProofAdded {
    public void audit(@Val ProofInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("CLAIM_PROOF_ADDED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " proofs=" + saved.proofs()
                + (input.kind() != null ? " kind=" + input.kind() : ""));
    }
}
