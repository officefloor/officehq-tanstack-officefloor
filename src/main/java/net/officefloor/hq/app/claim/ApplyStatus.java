package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the new status, refusing a change its current status does not allow. Opening
 *  gives the claim the sheet's next number and records who opened it; a voided claim keeps its number, which is never reused. Settling fixes its due at its person's
 *  grade rate in force on the claim's date, rounded half to even and at least the sheet's floor (none when no rate of the grade applies then),
 *  or at the figure its person's last statement bound it to, plus the late charge it has then (frozen in).
 *  The user who opened a claim may not settle it. */
public class ApplyStatus {
    public void apply(@Val StatusInput input, @Val Claim claim, ClaimStore store, PersonStore people,
            RateStore rates, SettingStore settings, SheetStore sheets, Out<StatusChange> change)
            throws InvalidSheetException {
        ClaimStatus from = claim.getStatus();
        if (input.status() == ClaimStatus.SETTLED && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(input.user())) {
            throw new InvalidSheetException("The user who opened a claim cannot settle it");
        }
        if (!from.canBecome(input.status()) || !store.move(claim, from, input.status())) {
            throw new InvalidSheetException("A " + from + " claim cannot become " + input.status());
        }
        // The due to freeze is worked out while the claim is still OPEN, so it takes its late charge in.
        BigDecimal settledDue = null;
        if (input.status() == ClaimStatus.SETTLED) {
            Person person = people.find(claim.getSheetId(), claim.getPersonId());
            BigDecimal pct = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
            settledDue = Lateness.of(claim.getSheetId(), settings, sheets).due(claim, pct, settings.floor(claim.getSheetId()));
        }
        claim.setStatus(input.status());
        if (input.status() == ClaimStatus.OPEN) {
            claim.setNumber(store.nextNumber(claim.getSheetId()));
            claim.setOpenedBy(input.user());
        }
        if (input.status() == ClaimStatus.SETTLED) {
            claim.setSettledDue(settledDue);
        }
        change.set(new StatusChange(claim.getNumber()));
    }
}
