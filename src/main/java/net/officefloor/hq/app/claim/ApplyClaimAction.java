package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Moves the claim to the action's status; refused (400) when the claim's status does not allow it.
 * Opening gives the claim the sheet's next number; a voided claim keeps its number (it is never reused). While the
 * sheet's period numbering from date is set and its today is on or after it, opening instead gives the claim the next
 * number within the period containing its date (P1-1, P1-2, ...), or within the claims in no period (X-1, ...).
 * Opening, settling and voiding are refused (400) to a role the sheet's permission matrix does not allow them.
 * Opening records the user who opened it; that user cannot settle it (four eyes). A deferred claim cannot be settled.
 * Settling fixes the claim's due at its person's grade's rate in force on the claim's date, raised to the sheet's floor and rounded by its policy date — or, when a statement
 * bound the claim, at the figure it was bound to — plus the late charge it has then, if overdue, frozen in; with no
 * figure it stays live. A clerk settling a claim whose due is above the sheet's clerk limit (a due of n/a is within any
 * limit) makes it PENDING instead, live and unfixed. A claim with no proofs whose due is above the sheet's proof threshold in
 * force on its date is refused settling or approving — before the clerk limit is considered, so it never goes PENDING. Only a steward approves a PENDING claim (fixing its due at its live
 * figure, plus any late charge, at that moment) — not the user who opened it — or rejects it, with a reason, back to OPEN.
 * Only a steward re-settles a SETTLED claim, fixing its due again at the figure it would have today as an open claim
 * bound by no statement (today's rates, grades, floor and late charge); its previous due is kept for the audit.
 */
public class ApplyClaimAction {
    public void apply(@PathVariable("action") String action, @RequestParam("user") String user, @Val Claim claim,
            @Val ClaimActionRequest request, SheetRepository sheets,
            PersonRepository people, RateRepository rates, SettingRepository settings, PeriodRepository periods,
            ClaimRepository claims, Out<ClaimAction> applied, Out<ClaimNumber> number, Out<PreviousDue> previous, Out<Claim> updated) throws InvalidClaimException {
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
        if (found == ClaimAction.SETTLE && claim.deferred(sheets.today(claim.sheetId()))) {
            throw new InvalidClaimException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be settled yet.");
        }
        if (found == ClaimAction.SETTLE && user.equals(claim.openedBy())) {
            throw new InvalidClaimException("The user who opened a claim cannot settle it.");
        }
        if (found == ClaimAction.APPROVE && user.equals(claim.openedBy())) {
            throw new InvalidClaimException("The user who opened a claim cannot approve it.");
        }
        if ((found == ClaimAction.APPROVE || found == ClaimAction.REJECT)
                && settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can " + action + " a pending claim.");
        }
        if (found == ClaimAction.RESETTLE && settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can re-settle a settled claim.");
        }
        if (found == ClaimAction.REJECT && request.reason().isEmpty()) {
            throw new InvalidClaimException("A reason is required to reject a claim.");
        }
        BigDecimal due = found.settles() ? settledDue(found == ClaimAction.RESETTLE ? claim.asOpenUnbound() : claim, sheets, people, rates, settings, claims) : claim.due();
        if ((found == ClaimAction.SETTLE || found == ClaimAction.APPROVE) && claim.proofs() == 0
                && settings.dated(claim.sheetId()).needsProof(claim.date(), due)) {
            throw new InvalidClaimException("The claim's due is above the proof threshold: it needs a proof to be "
                    + (found == ClaimAction.SETTLE ? "settled" : "approved") + ".");
        }
        if (found == ClaimAction.SETTLE && !settings.find(claim.sheetId()).maySettle(user, due)) {
            found = ClaimAction.PEND;
            due = null;
        }
        String next = switch (found) {
            case OPEN -> nextNumber(claim, sheets, settings, periods);
            default -> claim.number();
        };
        applied.set(found);
        previous.set(new PreviousDue(claim.due()));
        number.set(new ClaimNumber(next));
        updated.set(claim.withStatus(found.to(), next, due, found == ClaimAction.OPEN ? user : claim.openedBy()));
    }

    /** The number opening the claim gives it now: the sheet's next, or the next within its period under period numbering. */
    static String nextNumber(Claim claim, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods) {
        LocalDate from = settings.periodNumberingFrom(claim.sheetId());
        if (from == null || sheets.today(claim.sheetId()).isBefore(from)) {
            return String.valueOf(sheets.nextClaimNumber(claim.sheetId()));
        }
        Period period = periods.findAll(claim.sheetId()).stream().filter((p) -> p.contains(claim.date())).findFirst()
                .orElse(null);
        String prefix = period == null ? "X" : period.name();
        String key = period == null ? "" : period.name().toLowerCase();
        return prefix + "-" + sheets.nextPeriodClaimNumber(claim.sheetId(), key);
    }

    private static BigDecimal settledDue(Claim claim, SheetRepository sheets, PersonRepository people,
            RateRepository rates, SettingRepository settings, ClaimRepository claims) {
        return ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
    }
}
