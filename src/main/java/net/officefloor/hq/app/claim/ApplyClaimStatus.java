package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to the status the change takes it to, refusing a change its status does not allow; a settle
 *  with a date is as of that day. */
public class ApplyClaimStatus {
    public void apply(@PathVariable("sheet") String sheet, @PathVariable("change") String change,
            @RequestParam("user") String user, @RequestBody ClaimStatusRequest body, @Val Claim claim,
            ClaimStatusChanger changer, Claims claims, ClaimDueAsOf dueAsOf, SheetClock clock) throws InvalidClaimStatusChangeException {
        ClaimStatusChange requested = ClaimStatusChange.of(change);
        if (requested == null) {
            throw new InvalidClaimStatusChangeException("No such status change: " + change);
        }
        String on = body == null || body.on() == null ? "" : body.on().trim();
        if (requested == ClaimStatusChange.SETTLE && !on.isEmpty()) {
            // Settled as of a date on or before the sheet's today, at the due the claim had at the end of it.
            LocalDate date;
            try {
                date = LocalDate.parse(on);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimStatusChangeException("Not a valid settle date: " + on);
            }
            if (date.isAfter(clock.today(sheet))) {
                throw new InvalidClaimStatusChangeException("A claim cannot be settled as of a date after today");
            }
            ClaimDueAsOf.Then then = dueAsOf.on(sheet, claim.id(), date);
            if (!then.existed()) {
                throw new InvalidClaimStatusChangeException("The claim did not exist on " + date);
            }
            changer.settleAsOf(sheet, user, claim, date, then.due());
        } else {
            changer.change(sheet, user, claim, requested, body == null ? null : body.reason());
        }
        if (requested == ClaimStatusChange.SETTLE) {
            // Settled one at a time: no longer part of a bulk settle batch.
            claims.setBatch(sheet, claim.id(), null);
        }
    }
}
