package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_OPENED (CLAIM_REVIEW for an open that went to REVIEW; CLAIM_REVIEWED then CLAIM_OPENED for a review), CLAIM_SETTLED, CLAIM_PENDING, CLAIM_APPROVED, CLAIM_REJECTED, CLAIM_VOIDED, CLAIM_RESETTLED, CLAIM_DELETED or CLAIM_RESTORED to the audit
 * file once the status is stored, ending with number=<number> when the claim has one; CLAIM_SETTLED and CLAIM_APPROVED
 * then end with the due they fixed (due=n/a when its grade had no rate), CLAIM_REJECTED with reason=<reason>, CLAIM_RESETTLED with old=<previous due> new=<new due>, and CLAIM_VOIDED of a SETTLED claim with reversed=<its frozen due>, and a CLAIM_VOIDED given a void reason with reason=<reason> last. Then writes the
 * figures the change moved (see Rollup). A settle, approve or re-settle that leaves the claim SETTLED first writes, after
 * its own line, one NOTIFY user=<watcher> person=<name> number=<number> per user watching the claim's person, in the
 * order they started watching. */
public class RecordClaimStatusChanged {
    public void record(@Val Claim claim, @Val ClaimStatusChange change, @Val ClaimNumber number, @Val PreviousDue previous,
            PersonRepository people, WatchRepository watches, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String event = switch (change.action()) {
            case OPEN -> claim.status() == ClaimStatus.REVIEW ? "CLAIM_REVIEW" : "CLAIM_OPENED";
            case REVIEW -> "CLAIM_OPENED";
            case SETTLE -> claim.status() == ClaimStatus.PENDING ? "CLAIM_PENDING" : "CLAIM_SETTLED";
            case APPROVE -> "CLAIM_APPROVED";
            case REJECT -> "CLAIM_REJECTED";
            case VOID -> "CLAIM_VOIDED";
            case RESETTLE -> "CLAIM_RESETTLED";
            case DELETE -> "CLAIM_DELETED";
            case RESTORE -> "CLAIM_RESTORED";
        };
        Person person = people.find(claim.sheetId(), claim.personId());
        if (change.action() == ClaimAction.REVIEW) {
            audit.record("CLAIM_REVIEWED sheet=" + claim.sheetId() + " by=" + change.by() + " person="
                    + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                    + " date=" + claim.date());
        }
        audit.record(event + " sheet=" + claim.sheetId() + " by=" + change.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (number.value() == null ? "" : " number=" + number.value())
                + (change.action() == ClaimAction.RESETTLE
                        ? " old=" + figure(previous.value()) + " new=" + figure(claim.due())
                        : claim.status() == ClaimStatus.SETTLED
                        ? " due=" + (claim.due() == null ? "n/a" : claim.due().toPlainString()) : "")
                + (change.action() == ClaimAction.REJECT ? " reason=" + change.reason() : "")
                + (change.action() == ClaimAction.VOID && previous.status() == ClaimStatus.SETTLED
                        ? " reversed=" + figure(previous.value()) : "")
                + (change.action() == ClaimAction.VOID && !change.reason().isEmpty() ? " reason=" + change.reason() : ""));
        if (person != null && claim.status() == ClaimStatus.SETTLED && (change.action() == ClaimAction.SETTLE
                || change.action() == ClaimAction.APPROVE || change.action() == ClaimAction.RESETTLE)) {
            for (String watcher : watches.watchers(claim.sheetId(), person.id())) {
                audit.record("NOTIFY sheet=" + claim.sheetId() + " by=" + change.by() + " user=" + watcher + " person="
                        + person.name() + " number=" + number.value());
            }
        }
        rollup.record(claim.sheetId(), change.by(), before);
    }

    private static String figure(BigDecimal due) {
        return due == null ? "n/a" : due.toPlainString();
    }
}
