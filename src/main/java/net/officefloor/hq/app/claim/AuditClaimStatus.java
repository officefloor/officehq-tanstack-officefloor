package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.watch.Notify;
import net.officefloor.hq.app.watch.WatchStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored status change: CLAIM_OPENED, CLAIM_PENDING, CLAIM_SETTLED, CLAIM_APPROVED,
 *  CLAIM_REJECTED or CLAIM_VOIDED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>,
 *  then number=<n> when the claim has or had a number, then for CLAIM_SETTLED and CLAIM_APPROVED
 *  due=<0.00> as fixed on settling (n/a when its grade had no rate), for CLAIM_REJECTED reason=<reason>,
 *  for CLAIM_VOIDED of a SETTLED claim reversed=<0.00>, the frozen figure it takes out of the totals (n/a when none),
 *  then for CLAIM_VOIDED reason=<reason> when voided with one, then for CLAIM_SETTLED on=<date> when settled as of a date.
 *  A claim takes its number when settled or approved, so an opening has none. A claim opened above the review
 *  threshold is CLAIM_REVIEW; a steward's review of it is CLAIM_REVIEWED, then CLAIM_OPENED.
 *  A claim settled or approved then tells the person's watchers (NOTIFY), before the figures moved. */
public class AuditClaimStatus {
    public void audit(@Val StatusInput input, @Val StatusMove move, @Val Claim claim, @Val StatusChange change, @Val Rollup before,
            PersonStore people, Audit audit, RollupService rollups, WatchStore watches) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        if (move.from() == ClaimStatus.REVIEW && move.to() == ClaimStatus.OPEN) {
            String which = " sheet=" + claim.getSheetId() + " by=" + input.user() + " person=" + person.getName()
                    + " amount=" + shown.amount() + " date=" + shown.date();
            audit.record(move.to().event(move.from()) + which);
            audit.record(move.to().event() + which + (change.number() == null ? "" : " number=" + change.number()));
            rollups.record(claim.getSheetId(), input.user(), before);
            return;
        }
        audit.record(move.to().event(move.from()) + " sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (change.number() == null ? "" : " number=" + change.number())
                + (move.to() == ClaimStatus.SETTLED ? " due=" + (shown.due() == null ? "n/a" : shown.due()) : "")
                + (move.from() == ClaimStatus.PENDING && move.to() == ClaimStatus.OPEN ? " reason=" + input.reason() : "")
                + (move.from() == ClaimStatus.SETTLED && move.to() == ClaimStatus.VOID
                        ? " reversed=" + (shown.due() == null ? "n/a" : shown.due()) : "")
                + (move.to() == ClaimStatus.VOID && claim.getVoidReason() != null ? " reason=" + claim.getVoidReason() : "")
                + (move.to() == ClaimStatus.SETTLED && input.on() != null ? " on=" + input.on() : ""));
        if (move.to() == ClaimStatus.SETTLED) {
            Notify.watchers(watches, audit, person, input.user(), change.number());
        }
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
