package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.batch.SettleBatches;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyClaimStatus {
    // Stored only if the claim still has the status it was loaded with, so two users cannot both move it.
    // A PENDING claim moves only by a steward's decision: "approve" (to SETTLED) or "reject" (to OPEN, with a reason);
    // a decision is refused for any claim not PENDING. A PENDING claim can also be voided, like an OPEN one.
    // A REVIEW claim moves only by a steward's "review" (to OPEN, numbered then), or is voided.
    public void apply(@PathVariable("status") String status, @RequestBody(required = false) ClaimStatusRequest body,
            @Val Claim claim, Claims claims, People people, Rates rates, Settings settings, Sheets sheets,
            Periods periods, Rollup rollup, VoidReasons voidReasons, SettleBatches batches, ServerHttpConnection connection, Out<ClaimResponse> responseOut,
            Out<ClaimStatusRequest> requestOut) throws ClaimStatusRefusedException, ClaimProofNeededException, ClaimNoteNeededException,
            ClaimVoidReasonNeededException {
        ClaimStatus next;
        try {
            next = ClaimStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClaimStatusRefusedException("Not a claim status: " + status);
        }
        String user = Audit.user(connection).trim();
        String action = body == null || body.action() == null ? "" : body.action().trim();
        String reason = body == null || body.reason() == null ? "" : body.reason().trim();
        String voidReason = body == null || body.voidReason() == null ? "" : body.voidReason().trim();
        boolean review = action.equals("review");
        if (review && next != ClaimStatus.OPEN) {
            throw new ClaimStatusRefusedException("A review makes a claim OPEN.");
        }
        if (review && claim.status() != ClaimStatus.REVIEW) {
            throw new ClaimStatusRefusedException("Only a REVIEW claim is reviewed.");
        }
        if (!review && claim.status() == ClaimStatus.REVIEW && next != ClaimStatus.VOID) {
            throw new ClaimStatusRefusedException("A REVIEW claim is opened by a steward's review, or voided.");
        }
        boolean decision = !action.isEmpty() && !review;
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
        // Opening, settling and voiding are for the roles the sheet's permission matrix allows (approving and
        // rejecting stay with stewards, below).
        if (review && !steward) {
            throw new ClaimStatusRefusedException("Only a steward can review a claim.");
        }
        String permitted = decision || review ? null : switch (next) {
            case OPEN -> "open";
            case SETTLED -> "settle";
            case VOID -> "void";
            default -> null;
        };
        if (permitted != null && !settings.permits(claim.sheetId(), permitted, user)) {
            throw new ClaimStatusRefusedException("Your role is not allowed to " + permitted + " a claim.");
        }
        if (next == ClaimStatus.REVIEW) {
            throw new ClaimStatusRefusedException("A claim goes to REVIEW only when it is opened above the review threshold.");
        }
        if (next == ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("A claim becomes PENDING only when a clerk settles it above the clerk limit.");
        }
        // Four eyes: the user who opened a claim cannot settle it, nor approve it.
        if (next == ClaimStatus.SETTLED && claim.openedBy() != null && claim.openedBy().equals(user)) {
            throw new ClaimStatusRefusedException(claim.status() == ClaimStatus.PENDING
                    ? "The user who opened a claim cannot approve it."
                    : "The user who opened a claim cannot settle it.");
        }
        // Nor can the user who last edited it.
        if (next == ClaimStatus.SETTLED && user.equals(claims.editedBy(claim))) {
            throw new ClaimStatusRefusedException(claim.status() == ClaimStatus.PENDING
                    ? "The user who last edited a claim cannot approve it."
                    : "The user who last edited a claim cannot settle it.");
        }
        // A deferred claim cannot be settled until the sheet's today reaches its deferred-until date.
        if (next == ClaimStatus.SETTLED && claim.deferredOn(sheets.today(claim.sheetId()))) {
            throw new ClaimStatusRefusedException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be settled yet.");
        }
        if (!claim.status().allows(next)) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        // A DELETED claim is restored to DRAFT only on the sheet's today it was deleted.
        if (claim.status() == ClaimStatus.DELETED && next == ClaimStatus.DRAFT) {
            LocalDate deletedOn = claims.deletedOn(claim);
            if (deletedOn == null || !deletedOn.equals(sheets.today(claim.sheetId()))) {
                throw new ClaimStatusRefusedException("A DELETED claim can be restored only on the day it was deleted.");
            }
        }
        // A SETTLED claim is voided only by a steward, and only within a week of its settlement (the status log's
        // SETTLED date) by the sheet's today.
        if (claim.status() == ClaimStatus.SETTLED && next == ClaimStatus.VOID) {
            if (!steward) {
                throw new ClaimStatusRefusedException("Only a steward can void a SETTLED claim.");
            }
            LocalDate settledOn = claims.settledOn(claim);
            if (settledOn == null || sheets.today(claim.sheetId())
                    .isAfter(settledOn.plusDays(ClaimStatus.VOID_SETTLED_DAYS))) {
                throw new ClaimStatusRefusedException("A SETTLED claim can be voided only within "
                        + ClaimStatus.VOID_SETTLED_DAYS + " days of its settlement.");
            }
        }
        if (claim.status() == ClaimStatus.PENDING && (next == ClaimStatus.SETTLED || next == ClaimStatus.OPEN)
                && !steward) {
            throw new ClaimStatusRefusedException("Only a steward can approve or reject a pending claim.");
        }
        if (claim.status() == ClaimStatus.PENDING && next == ClaimStatus.OPEN && reason.isEmpty()) {
            throw new ClaimStatusRefusedException("Give a reason to reject the claim.");
        }
        // While the sheet has void reasons, a claim is voided only with one of them; with none, no reason is kept.
        if (next == ClaimStatus.VOID) {
            List<String> reasons = voidReasons.list(claim.sheetId());
            if (reasons.isEmpty()) {
                voidReason = "";
            } else if (!reasons.contains(voidReason)) {
                throw new ClaimVoidReasonNeededException(voidReason.isEmpty()
                        ? "Choose a void reason to void the claim."
                        : "\"" + voidReason + "\" is not one of the sheet's void reasons.");
            }
        }
        // Settling (or approving) freezes the due as shown: the one a statement bound, else the late charge on the sheet's today into the due.
        BigDecimal due = claim.stated() ? claim.statedDue()
                : settings.withLate(claim, due(claim, people, rates, settings), sheets.today(claim.sheetId()));
        // A claim whose due is above the proof threshold in force on its date (its person's grade's, else the sheet's) is settled or approved only once it has a
        // proof, whoever settles it, and before the clerk limit is considered (a due of n/a is within any threshold).
        // While the sheet has proof kinds, such a claim needs at least one proof of each of them instead.
        if (next == ClaimStatus.SETTLED && due != null) {
            java.util.List<String> kinds = settings.proofKinds(claim.sheetId());
            java.util.List<String> missing = kinds.isEmpty() ? (claim.proofs() == 0 ? java.util.List.of("") : java.util.List.of())
                    : kinds.stream().filter(kind -> claim.proofsOf(kind) == 0).toList();
            BigDecimal threshold = missing.isEmpty() ? null : settings.proofThreshold(claim);
            if (threshold != null && due.compareTo(threshold) > 0) {
                throw new ClaimProofNeededException("The claim's due is above the proof threshold (" + threshold.toPlainString()
                        + "): add " + (kinds.isEmpty() ? "a proof" : "a " + String.join(" and a ", missing))
                        + " before it is " + (claim.status() == ClaimStatus.PENDING ? "approved." : "settled."));
            }
        }
        // A claim whose due is above the note threshold in force on its date is opened only once it has a note (a due
        // of n/a is within any threshold).
        if (next == ClaimStatus.OPEN && claim.status() == ClaimStatus.DRAFT && due != null
                && (claim.note() == null || claim.note().isBlank())) {
            BigDecimal threshold = settings.noteThreshold(claim.sheetId(), claim.date());
            if (threshold != null && due.compareTo(threshold) > 0) {
                throw new ClaimNoteNeededException("The claim's due is above the note threshold (" + threshold.toPlainString()
                        + "): give it a note before it is opened.");
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
        // A DRAFT claim belonging to a period that caps its claims (its due-by falls in it) is opened only while its
        // person has fewer OPEN or PENDING claims in the period than the cap.
        if (next == ClaimStatus.OPEN && claim.status() == ClaimStatus.DRAFT) {
            Period capped = periods.on(claim.sheetId(), claim.dueBy());
            if (capped != null && capped.cap() != null
                    && claims.openInPeriod(claim.sheetId(), claim.personId(), capped) >= capped.cap()) {
                throw new ClaimStatusRefusedException("The person already has " + capped.cap()
                        + " open or pending claims in the period " + capped.name() + ", its cap.");
            }
        }
        // A DRAFT claim whose due is above the review threshold in force on its date goes to REVIEW instead of OPEN
        // (a due of n/a is within any threshold): it is numbered only when a steward's review makes it OPEN.
        if (next == ClaimStatus.OPEN && claim.status() == ClaimStatus.DRAFT && due != null) {
            BigDecimal threshold = settings.reviewThreshold(claim.sheetId(), claim.date());
            if (threshold != null && due.compareTo(threshold) > 0) {
                next = ClaimStatus.REVIEW;
            }
        }
        // While the sheet numbers by period (its today on or after the from-date), an opened claim is numbered
        // within the period its due-by falls in, or in the no-period sequence; otherwise sheet-wide.
        String scope = null;
        LocalDate numberingFrom = settings.periodNumberingFrom(claim.sheetId());
        if (next == ClaimStatus.OPEN && numberingFrom != null && !sheets.today(claim.sheetId()).isBefore(numberingFrom)) {
            Period period = periods.on(claim.sheetId(), claim.dueBy());
            scope = period == null ? "" : period.name();
        }
        Claim changed = claims.changeStatus(claim, next, due, user, scope);
        if (changed == null) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        // Settled one at a time, the claim leaves any batch that settled it before (a bulk settle records its batch after).
        if (!decision && (next == ClaimStatus.SETTLED || next == ClaimStatus.PENDING)) {
            batches.remove(changed.id());
        }
        if (next == ClaimStatus.VOID && !voidReason.isEmpty()) {
            voidReasons.voided(changed.id(), voidReason);
        }
        claims.log(changed, user, sheets.today(claim.sheetId()), rollup.figure(changed));
        responseOut.set(ClaimResponse.from(changed));
        requestOut.set(new ClaimStatusRequest(action, reason, voidReason));
    }

    /**
     * The unbound claim's due now, before its late charge: its amount times its
     * person's grade's rate on the claim's date, rounded by the policy date (not yet floored); null when none applies.
     */
    private static BigDecimal due(Claim claim, People people, Rates rates, Settings settings) {
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person == null) {
            return null;
        }
        BigDecimal rate = rates.forClaim(claim, person.gradeOn(claim.date()));
        return settings.live(claim, rate);
    }
}
