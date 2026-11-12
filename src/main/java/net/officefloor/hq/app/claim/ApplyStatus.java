package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the new status, refusing a change its current status does not allow. Opening
 *  gives the claim its next number (see {@link Numbering}) and records who opened it; a voided claim keeps its number, which is never reused. Settling fixes its due at its person's
 *  grade rate in force on the claim's date, rounded as the sheet's policy date has it and at least the sheet's floor (none when no rate of the grade applies then),
 *  or at the figure its person's last statement bound it to, plus the late charge it has then (frozen in).
 *  A clerk's settle above the clerk limit makes it PENDING instead (see {@link CheckClerkLimit}); a steward's
 *  approval settles a PENDING claim, frozen then at its live figure (a statement does not bind it while
 *  PENDING), and a rejection moves it back to OPEN, bound again by its person's last statement if that included it.
 *  The user who opened a claim may not settle or approve it. */
public class ApplyStatus {
    public void apply(@Val StatusInput input, @Val StatusMove move, @Val Claim claim, ClaimStore store, PersonStore people,
            RateStore rates, SettingStore settings, SheetStore sheets, PeriodStore periods,
            Out<StatusChange> change)
            throws InvalidSheetException {
        ClaimStatus from = move.from();
        ClaimStatus to = move.to();
        if (input.status() == ClaimStatus.SETTLED && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(input.user())) {
            throw new InvalidSheetException("The user who opened a claim cannot "
                    + (input.review() ? "approve" : "settle") + " it");
        }
        if (!from.canBecome(to) || !store.move(claim, from, to)) {
            throw new InvalidSheetException("A " + from + " claim cannot become " + to);
        }
        // The due to freeze is worked out while the claim is still OPEN or PENDING, so it takes its late charge in.
        BigDecimal settledDue = null;
        if (to == ClaimStatus.SETTLED) {
            Person person = people.find(claim.getSheetId(), claim.getPersonId());
            BigDecimal pct = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
            settledDue = Lateness.of(claim.getSheetId(), settings, sheets, store).due(claim, pct, settings.floor(claim.getSheetId()));
        }
        claim.setStatus(to);
        if (from == ClaimStatus.DRAFT && to == ClaimStatus.OPEN) {
            claim.setNumber(Numbering.next(claim.getSheetId(), claim.getDate(), store, settings, sheets, periods));
            claim.setOpenedBy(input.user());
        }
        if (to == ClaimStatus.SETTLED) {
            claim.setSettledDue(settledDue);
        }
        change.set(new StatusChange(claim.getNumber()));
    }
}
