package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.voidreason.VoidReasonStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the new status, refusing a change its current status does not allow. Opening
 *  gives the claim its next number (see {@link Numbering}) and records who opened it; a voided claim keeps its number, which is never reused. Settling fixes its due at its person's
 *  grade rate in force on the claim's date, rounded as the sheet's policy date has it (none when no rate of the grade applies then),
 *  or at the figure its person's last statement bound it to, plus the late charge it has then, and at least the sheet's floor (frozen in).
 *  A clerk's settle above the clerk limit makes it PENDING instead (see {@link CheckClerkLimit}); a steward's
 *  approval settles a PENDING claim, frozen then at its live figure (a statement does not bind it while
 *  PENDING), and a rejection moves it back to OPEN, bound again by its person's last statement if that included it.
 *  The user who opened a claim, or who last edited it, may not settle or approve it, and a claim deferred on the sheet's today may not be settled.
 *  Opening a DRAFT is refused while its person already has as many OPEN or PENDING claims in its period as the period's cap.
 *  A claim opened to REVIEW records who opened it but takes no number until a steward's review makes it OPEN.
 *  While the sheet has void reasons, a void needs one of them (the input's reason), kept on the claim. */
public class ApplyStatus {
    public void apply(@Val StatusInput input, @Val StatusMove move, @Val Claim claim, ClaimStore store, PersonStore people,
            RateStore rates, SettingStore settings, SheetStore sheets, PeriodStore periods,
            VoidReasonStore voidReasons, Out<StatusChange> change)
            throws InvalidSheetException {
        ClaimStatus from = move.from();
        ClaimStatus to = move.to();
        if (input.status() == ClaimStatus.SETTLED && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(input.user())) {
            throw new InvalidSheetException("The user who opened a claim cannot "
                    + (input.review() ? "approve" : "settle") + " it");
        }
        if (input.status() == ClaimStatus.SETTLED && claim.getEditedBy() != null
                && claim.getEditedBy().equals(input.user())) {
            throw new InvalidSheetException("The user who last edited a claim cannot "
                    + (input.review() ? "approve" : "settle") + " it");
        }
        if (input.status() == ClaimStatus.SETTLED && claim.isDeferredOn(sheets.today(claim.getSheetId()))) {
            throw new InvalidSheetException("A deferred claim cannot be settled until " + claim.getDeferredUntil());
        }
        String voidReason = null;
        if (to == ClaimStatus.VOID) {
            List<String> reasons = voidReasons.list(claim.getSheetId());
            if (!reasons.isEmpty()) {
                if (input.reason().isEmpty()) {
                    throw new InvalidSheetException("A void needs a reason from the sheet's list");
                }
                if (!reasons.contains(input.reason())) {
                    throw new InvalidSheetException(input.reason() + " is not a void reason on the sheet");
                }
                voidReason = input.reason();
            }
        }
        if (from == ClaimStatus.DRAFT && (to == ClaimStatus.OPEN || to == ClaimStatus.REVIEW)) {
            ClaimPeriods.refuseCapped(periods, store, claim);
        }
        if (!from.canBecome(to) || !store.move(claim, from, to)) {
            throw new InvalidSheetException("A " + from + " claim cannot become " + to);
        }
        // The due to freeze is worked out while the claim is still OPEN or PENDING, so it takes its late charge in.
        BigDecimal settledDue = null;
        if (to == ClaimStatus.SETTLED) {
            Person person = people.find(claim.getSheetId(), claim.getPersonId());
            BigDecimal pct = person == null ? null : rates.forClaim(claim).find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
            settledDue = Lateness.of(claim.getSheetId(), settings, sheets, store, people).due(claim, pct);
        }
        claim.setStatus(to);
        if (to == ClaimStatus.VOID) {
            claim.setVoidReason(voidReason);
        }
        if ((from == ClaimStatus.DRAFT || from == ClaimStatus.REVIEW) && to == ClaimStatus.OPEN) {
            claim.setNumber(Numbering.next(claim.getSheetId(), claim.getDueBy(), store, settings, sheets, periods));
        }
        if (from == ClaimStatus.DRAFT && (to == ClaimStatus.OPEN || to == ClaimStatus.REVIEW)) {
            claim.setOpenedBy(input.user());
        }
        if (to == ClaimStatus.SETTLED) {
            claim.setSettledDue(settledDue);
        }
        change.set(new StatusChange(claim.getNumber()));
    }
}
