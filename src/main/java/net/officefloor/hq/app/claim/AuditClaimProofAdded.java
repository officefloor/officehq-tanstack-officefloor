package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the proof added; the line ends with the claim's proofs count now. */
public class AuditClaimProofAdded {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_PROOF_ADDED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim) + " proofs=" + claim.getProofs());
    }
}
