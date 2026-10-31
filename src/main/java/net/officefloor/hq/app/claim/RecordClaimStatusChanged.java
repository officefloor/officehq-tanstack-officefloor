package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.plugin.variable.Val;

/** Runs after the claim is saved, so a refused status change writes no audit line. */
public class RecordClaimStatusChanged {
    public void record(@Val ClaimStatusChange change, @Val Claim claim, People people, ClaimLog log, Watches watches, Audit audit) {
        log.write(claim, change.user());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        // A clerk's settle over the clerk limit leaves the claim PENDING: it settles nothing yet.
        boolean pending = change.action() == ClaimAction.SETTLE && claim.getStatus() == ClaimStatus.PENDING;
        boolean fixesDue = !pending && (change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE);
        // Only a claim that was SETTLED has a SETTLED log line, so a void of one reverses its frozen due.
        boolean voidsSettled = change.action() == ClaimAction.VOID && log.settledOn(claim) != null;
        audit.record((pending ? "CLAIM_PENDING" : change.action().event()) + " sheet=" + claim.getSheetId() + " by=" + change.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : "")
                + (fixesDue ? " due=" + (claim.getDue() != null ? claim.getDue().toPlainString() : "n/a") : "")
                + (change.action() == ClaimAction.REJECT ? " reason=" + change.reason() : "")
                + (voidsSettled ? " reversed=" + (claim.getDue() != null ? claim.getDue().toPlainString() : "n/a") : ""));
        // A watched person's claim settled or approved: a line per watching user, after the claim's own.
        if (fixesDue) watches.notify(claim, person, change.user(), audit);
    }
}
