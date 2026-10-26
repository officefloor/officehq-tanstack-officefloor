package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyClaimStatus {
    // Stored only if the claim still has the status it was loaded with, so two users cannot both move it.
    // A PENDING claim moves only by a steward's decision: "approve" (to SETTLED) or "reject" (to OPEN, with a reason);
    // a decision is refused for any claim not PENDING. A PENDING claim can also be voided, like an OPEN one.
    public void apply(@PathVariable("status") String status, @RequestBody(required = false) ClaimStatusRequest body,
            @Val Claim claim, Claims claims, People people, Rates rates, Settings settings, Sheets sheets,
            Periods periods, Rollup rollup, ServerHttpConnection connection, Out<ClaimResponse> responseOut,
            Out<ClaimStatusRequest> requestOut) throws ClaimStatusRefusedException, ClaimProofNeededException {
        ClaimStatus next;
        try {
            next = ClaimStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClaimStatusRefusedException("Not a claim status: " + status);
        }
        String user = Audit.user(connection).trim();
        String action = body == null || body.action() == null ? "" : body.action().trim();
        String reason = body == null || body.reason() == null ? "" : body.reason().trim();
        boolean decision = !action.isEmpty();
        if (decision && !(action.equals("approve") && next == ClaimStatus.SETTLED)
                && !(action.equals("reject") && next == ClaimStatus.OPEN)) {
            throw new ClaimStatusRefusedException("Not a decision on a pending claim: " + action);
        }
        if (decision && claim.status() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only a PENDING claim is approved or rejected.");
        }
        if (!decision && claim.status() == ClaimStatus.PENDING && next != ClaimStatus.VOID) {
            throw new ClaimStatusRefusedException("A PENDING claim is approved or rejected by a steward, or voided.");
        }
        boolean steward = Settings.STEWARD.equals(settings.role(claim.sheetId(), user));
        if (next == ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("A claim becomes PENDING only when a clerk settles it above the clerk limit.");
        }
        // Four eyes: the user who opened a claim cannot settle it, nor approve it.
        if (next == ClaimStatus.SETTLED && claim.openedBy() != null && claim.openedBy().equals(user)) {
            throw new ClaimStatusRefusedException(claim.status() == ClaimStatus.PENDING
                    ? "The user who opened a claim cannot approve it."
                    : "The user who opened a claim cannot settle it.");
        }
        // A deferred claim cannot be settled until the sheet's today reaches its deferred-until date.
        if (next == ClaimStatus.SETTLED && claim.deferredOn(sheets.today(claim.sheetId()))) {
            throw new ClaimStatusRefusedException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be settled yet.");
        }
        if (!claim.status().allows(next)) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        if (claim.status() == ClaimStatus.PENDING && (next == ClaimStatus.SETTLED || next == ClaimStatus.OPEN)
                && !steward) {
            throw new ClaimStatusRefusedException("Only a steward can approve or reject a pending claim.");
        }
        if (claim.status() == ClaimStatus.PENDING && next == ClaimStatus.OPEN && reason.isEmpty()) {
            throw new ClaimStatusRefusedException("Give a reason to reject the claim.");
        }
        // Settling (or approving) freezes the late charge on the sheet's today into the due.
        BigDecimal due = settings.withLate(claim, due(claim, people, rates, settings), sheets.today(claim.sheetId()));
        // A claim whose due is above the proof threshold in force on its date is settled or approved only once it has a
        // proof, whoever settles it, and before the clerk limit is considered (a due of n/a is within any threshold).
        if (next == ClaimStatus.SETTLED && claim.proofs() == 0 && due != null) {
            BigDecimal threshold = settings.proofThreshold(claim.sheetId(), claim.date());
            if (threshold != null && due.compareTo(threshold) > 0) {
                throw new ClaimProofNeededException("The claim's due is above the proof threshold (" + threshold.toPlainString()
                        + "): add a proof before it is " + (claim.status() == ClaimStatus.PENDING ? "approved." : "settled."));
            }
        }
        // A clerk settles only a claim whose due is at or below the clerk limit (a due of n/a is within any
        // limit); above it the claim goes PENDING, for a steward to approve. A steward settles anything.
        if (next == ClaimStatus.SETTLED && claim.status() == ClaimStatus.OPEN && due != null && !steward) {
            BigDecimal limit = settings.clerkLimit(claim.sheetId());
            if (limit != null && due.compareTo(limit) > 0) {
                next = ClaimStatus.PENDING;
            }
        }
        // While the sheet numbers by period (its today on or after the from-date), an opened claim is numbered
        // within the period its date falls in, or in the no-period sequence; otherwise sheet-wide.
        String scope = null;
        LocalDate numberingFrom = settings.periodNumberingFrom(claim.sheetId());
        if (next == ClaimStatus.OPEN && numberingFrom != null && !sheets.today(claim.sheetId()).isBefore(numberingFrom)) {
            Period period = periods.on(claim.sheetId(), claim.date());
            scope = period == null ? "" : period.name();
        }
        Claim changed = claims.changeStatus(claim, next, due, user, scope);
        if (changed == null) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        claims.log(changed, user, sheets.today(claim.sheetId()), rollup.figure(changed));
        responseOut.set(ClaimResponse.from(changed));
        requestOut.set(new ClaimStatusRequest(action, reason));
    }

    /**
     * The claim's due now, before its late charge: the figure its person's last statement bound it to (a PENDING
     * claim is not bound: its figure is live), otherwise its amount times its
     * person's grade's rate on the claim's date, rounded by the policy date, raised to the floor; null when none applies.
     */
    private static BigDecimal due(Claim claim, People people, Rates rates, Settings settings) {
        if (claim.stated() && claim.status() != ClaimStatus.PENDING) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person == null) {
            return null;
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.gradeOn(claim.date()), claim.date());
        return settings.live(claim, rate);
    }
}
