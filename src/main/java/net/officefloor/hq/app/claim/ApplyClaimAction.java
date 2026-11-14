package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Moves the claim to the action's status; refused (400) when the claim's status does not allow it.
 * Opening gives the claim the sheet's next number; a voided claim keeps its number (it is never reused). While the
 * sheet's period numbering from date is set and its today is on or after it, opening instead gives the claim the next
 * number within the period containing its due-by (P1-1, P1-2, ...), or within the claims in no period (X-1, ...).
 * Opening, settling and voiding are refused (400) to a role the sheet's permission matrix does not allow them.
 * Opening records the user who opened it; that user cannot settle it (four eyes). Nor can the user who last edited
 * it (through its editor or its row's amount entry) settle or approve it. A deferred claim cannot be settled.
 * Settling fixes the claim's due at the greater of the sheet's floor in force on its date and its figure — at its person's grade's rate in force on the claim's date,
 * rounded by its policy date, or, when a statement bound the claim, the figure it was bound to — plus the late charge it has then, if overdue, frozen in; with no
 * figure it stays live. A clerk settling a claim whose due is above the sheet's clerk limit (a due of n/a is within any
 * limit) makes it PENDING instead, live and unfixed. A claim with no proofs (while the sheet has proof kinds: without a proof of each) whose due is above the proof threshold in force on its date —
 * its person's grade's own, when that grade has one then, else the sheet's — is refused settling or approving — before the clerk limit is considered, so it never goes PENDING.
 * A claim with an empty note whose due (as shown now) is above the sheet's note threshold in force on its date is refused opening.
 * Opening a claim due by a date in a period with a cap is refused while its person already has that many OPEN or PENDING
 * claims due by a date in that period. Only a steward approves a PENDING claim (fixing its due at its figure —
 * the one its statement bound it to, if any — plus any late charge, at that moment) — not the user who opened it — or rejects it, with a reason, back to OPEN.
 * Only a steward re-settles a SETTLED claim, fixing its due again at the figure it would have today as an open claim
 * bound by no statement (today's rates, grades, floor and late charge); its previous due is kept for the audit.
 * Only a steward voids a SETTLED claim, and only while the sheet's today is within 7 days of its settlement (the
 * status log's latest SETTLED date); it keeps its number and its frozen due, which leaves every total.
 * While the sheet's void reasons list is not empty, a void is refused (400) without a reason chosen from it; a reason
 * not on the list is always refused.
 * Opening a claim whose due (as shown now) is above the sheet's review threshold in force on its date sends it to REVIEW
 * instead, with no number yet, recording who opened it; only a steward reviews it, which opens it (giving its number).
 * While its person is flagged (the sheet's today on or before the flag's until date), only a steward opens a claim.
 * Deleting sets a DRAFT claim aside as DELETED (counting nowhere); restoring makes it a DRAFT again, only while the
 * sheet's today is the day it was deleted (the status log's latest DELETED date).
 */
public class ApplyClaimAction {

    /** How many days after its settlement (the status log's SETTLED date) a steward may still void a SETTLED claim. */
    static final int VOID_SETTLED_DAYS = 7;

    public void apply(@PathVariable("action") String action, @RequestParam("user") String user, @Val Claim claim,
            @Val ClaimActionRequest request, SheetRepository sheets,
            PersonRepository people, RateRepository rates, SettingRepository settings, PeriodRepository periods,
            VoidReasonRepository voidReasons, ClaimRepository claims, ClaimLogRepository logs, Out<ClaimAction> applied, Out<ClaimNumber> number, Out<PreviousDue> previous, Out<Claim> updated) throws InvalidClaimException {
        ClaimAction found = ClaimAction.of(action);
        if (found == null) {
            throw new InvalidClaimException("No such claim action: " + action);
        }
        if (!found.allowedFrom(claim.status())) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be " + found.to() + ".");
        }
        if ((found == ClaimAction.OPEN || found == ClaimAction.SETTLE || found == ClaimAction.VOID)
                && !settings.find(claim.sheetId()).may(user, action)) {
            throw new InvalidClaimException("The user's role is not allowed to " + action + " a claim.");
        }
        if (found == ClaimAction.OPEN && settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD
                && people.flagOn(claim.sheetId(), claim.personId(), sheets.today(claim.sheetId())) != null) {
            throw new InvalidClaimException("The claim's person is flagged: only a steward may open their claims.");
        }
        if (found == ClaimAction.SETTLE && claim.deferred(sheets.today(claim.sheetId()))) {
            throw new InvalidClaimException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be settled yet.");
        }
        if (found == ClaimAction.SETTLE && user.equals(claim.openedBy())) {
            throw new InvalidClaimException("The user who opened a claim cannot settle it.");
        }
        if (found == ClaimAction.APPROVE && user.equals(claim.openedBy())) {
            throw new InvalidClaimException("The user who opened a claim cannot approve it.");
        }
        if ((found == ClaimAction.SETTLE || found == ClaimAction.APPROVE)
                && user.equals(claims.editedBy(claim.sheetId(), claim.id()))) {
            throw new InvalidClaimException("The user who last edited a claim cannot " + action + " it.");
        }
        if ((found == ClaimAction.APPROVE || found == ClaimAction.REJECT)
                && settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can " + action + " a pending claim.");
        }
        if (found == ClaimAction.REVIEW && settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can review a claim.");
        }
        if (found == ClaimAction.RESETTLE && settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can re-settle a settled claim.");
        }
        if (found == ClaimAction.VOID && claim.status() == ClaimStatus.SETTLED) {
            if (settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
                throw new InvalidClaimException("Only a steward can void a settled claim.");
            }
            LocalDate settledOn = logs.find(claim.sheetId(), claim.id()).stream()
                    .filter((l) -> l.status() == ClaimStatus.SETTLED).map(ClaimLogLine::date)
                    .reduce((a, b) -> b).orElse(null);
            if (settledOn == null || sheets.today(claim.sheetId()).isAfter(settledOn.plusDays(VOID_SETTLED_DAYS))) {
                throw new InvalidClaimException("A settled claim can be voided only within " + VOID_SETTLED_DAYS
                        + " days of its settlement.");
            }
        }
        if (found == ClaimAction.VOID) {
            List<String> reasons = voidReasons.findAll(claim.sheetId());
            if (request.reason().isEmpty() ? !reasons.isEmpty() : !reasons.contains(request.reason())) {
                throw new InvalidClaimException("A void needs a reason chosen from the sheet's void reasons.");
            }
        }
        if (found == ClaimAction.RESTORE) {
            LocalDate deletedOn = logs.find(claim.sheetId(), claim.id()).stream()
                    .filter((l) -> l.status() == ClaimStatus.DELETED).map(ClaimLogLine::date)
                    .reduce((a, b) -> b).orElse(null);
            if (deletedOn == null || !sheets.today(claim.sheetId()).equals(deletedOn)) {
                throw new InvalidClaimException("A deleted claim can be restored only on the day it was deleted.");
            }
        }
        if (found == ClaimAction.REJECT && request.reason().isEmpty()) {
            throw new InvalidClaimException("A reason is required to reject a claim.");
        }
        BigDecimal due = found.settles() ? settledDue(found == ClaimAction.RESETTLE ? claim.asOpenUnbound() : claim, sheets, people, rates, settings, claims) : claim.due();
        if (found == ClaimAction.OPEN && (claim.note() == null || claim.note().isBlank())
                && settings.dated(claim.sheetId()).needsNote(claim.date(),
                        ClaimDueNow.of(claim, sheets, people, rates, settings, claims))) {
            throw new InvalidClaimException("The claim's due is above the note threshold: it needs a note to be opened.");
        }
        if (found == ClaimAction.OPEN) {
            checkCap(claim, periods, claims);
        }
        if ((found == ClaimAction.SETTLE || found == ClaimAction.APPROVE) && !hasProofs(claim, settings, claims)
                && settings.dated(claim.sheetId()).needsProof(proofGrade(claim, people), claim.date(), due)) {
            throw new InvalidClaimException("The claim's due is above the proof threshold: it needs a proof (of each of the sheet's proof kinds) to be "
                    + (found == ClaimAction.SETTLE ? "settled" : "approved") + ".");
        }
        if (found == ClaimAction.OPEN && settings.dated(claim.sheetId()).needsReview(claim.date(),
                ClaimDueNow.of(claim, sheets, people, rates, settings, claims))) {
            found = ClaimAction.SEND_TO_REVIEW;
        }
        if (found == ClaimAction.SETTLE && !settings.find(claim.sheetId()).maySettle(user, due)) {
            found = ClaimAction.PEND;
            due = null;
        }
        String next = switch (found) {
            case OPEN, REVIEW -> ClaimNumbers.next(claim, sheets, settings, periods);
            default -> claim.number();
        };
        applied.set(found);
        previous.set(new PreviousDue(claim.due(), claim.status() == ClaimStatus.SETTLED));
        number.set(new ClaimNumber(next));
        updated.set(claim.withStatus(found.to(), next, due, found == ClaimAction.OPEN || found == ClaimAction.SEND_TO_REVIEW ? user : claim.openedBy()));
    }

    /** Refuses opening the claim when its period's cap on its person's OPEN and PENDING claims is already reached. */
    private static void checkCap(Claim claim, PeriodRepository periods, ClaimRepository claims) throws InvalidClaimException {
        Period period = periods.findAll(claim.sheetId()).stream()
                .filter((p) -> p.cap() != null && claim.dueBy() != null && p.contains(claim.dueBy())).findFirst().orElse(null);
        if (period == null) {
            return;
        }
        long open = claims.findForPerson(claim.sheetId(), claim.personId()).stream()
                .filter((c) -> !c.id().equals(claim.id()) && c.dueBy() != null && period.contains(c.dueBy())
                        && (c.status() == ClaimStatus.OPEN || c.status() == ClaimStatus.PENDING))
                .count();
        if (open >= period.cap()) {
            throw new InvalidClaimException("The person already has " + period.cap() + " open claims in period "
                    + period.name() + ": its cap.");
        }
    }

    /** Whether the claim has the proofs a claim above the proof threshold needs: one of each of the sheet's proof kinds, else any one. */
    static boolean hasProofs(Claim claim, SettingRepository settings, ClaimRepository claims) {
        if (claim.proofs() == 0) {
            return false;
        }
        int photos = claims.photoProofs(claim.sheetId(), claim.id());
        for (String kind : settings.proofKinds(claim.sheetId())) {
            int count = ProofKind.PHOTO.equals(kind) ? photos : claim.proofs() - photos;
            if (count == 0) {
                return false;
            }
        }
        return true;
    }

    /** The grade whose proof threshold the claim falls under: its person's grade in force on its date (null: no person). */
    static String proofGrade(Claim claim, PersonRepository people) {
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? null : person.gradeOn(claim.date());
    }

    private static BigDecimal settledDue(Claim claim, SheetRepository sheets, PersonRepository people,
            RateRepository rates, SettingRepository settings, ClaimRepository claims) {
        return ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
    }
}
