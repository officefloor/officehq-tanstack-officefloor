package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the new status, refusing a change its current status does not allow. Opening
 *  gives the claim the sheet's next number; voiding clears it. Settling fixes its due at its person's
 *  grade rate in force on the claim's date (none when no rate of the grade applies then). */
public class ApplyStatus {
    public void apply(@Val StatusInput input, @Val Claim claim, ClaimStore store, PersonStore people,
            RateStore rates, Out<StatusChange> change)
            throws InvalidSheetException {
        ClaimStatus from = claim.getStatus();
        if (!from.canBecome(input.status()) || !store.move(claim, from, input.status())) {
            throw new InvalidSheetException("A " + from + " claim cannot become " + input.status());
        }
        claim.setStatus(input.status());
        if (input.status() == ClaimStatus.OPEN) {
            claim.setNumber(store.nextNumber(claim.getSheetId()));
        }
        if (input.status() == ClaimStatus.SETTLED) {
            Person person = people.find(claim.getSheetId(), claim.getPersonId());
            BigDecimal pct = person == null ? null : rates.find(claim.getSheetId(), person.getGrade(), claim.getDate());
            claim.setSettledDue(pct == null ? null
                    : claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        change.set(new StatusChange(claim.getNumber()));
        if (input.status() == ClaimStatus.VOID) {
            claim.setNumber(null);
        }
    }
}
