package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim's status change, once it is saved, with the claim's number when it has one (a review is
 * recorded as CLAIM_REVIEWED, then CLAIM_OPENED with the number it took);
 * a settle or approve adds the due it fixed (n/a when the grade had no rate), a reject its reason, and voiding a
 * settled claim the frozen figure it takes out of the totals (reversed=), then the reason it was voided with when one was given. */
public class AuditClaimStatus {
    public void audit(@Parameter String number, @Val Claim claim, PersonRepository people,
            @PathVariable("action") String action, ClaimLogRepository log,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        String event = switch (action) {
            case "approve" -> "CLAIM_APPROVED";
            case "reject" -> "CLAIM_REJECTED";
            case "restore" -> "CLAIM_RESTORED";
            default -> claim.getStatus().event();
        };
        if ("review".equals(action)) {
            audit.record(ClaimAudit.line("CLAIM_REVIEWED", claim, person, AuditUser.decode(user), null));
        }
        String line = ClaimAudit.line(event, claim, person, AuditUser.decode(user), number);
        if ("reject".equals(action)) {
            line += " reason=" + reason.trim();
        }
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getDue() == null ? "n/a" : claim.getDue().setScale(2).toPlainString());
        }
        if (claim.getStatus() == ClaimStatus.VOID && log.settledOn(claim) != null) {
            line += " reversed=" + (claim.getDue() == null ? "n/a" : claim.getDue().setScale(2).toPlainString());
        }
        if (claim.getStatus() == ClaimStatus.VOID && claim.getVoidReason() != null) {
            line += " reason=" + claim.getVoidReason();
        }
        audit.record(line);
    }
}
