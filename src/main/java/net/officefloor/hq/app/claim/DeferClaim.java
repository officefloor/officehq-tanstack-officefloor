package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Defers an OPEN claim until a date after the sheet's today. Refused for a claim due by a day in a closed
 *  period, for a claim that is not OPEN, and for a date that is not a date or not after today. */
public class DeferClaim {
    public void defer(@RequestParam(name = "until", required = false) String until, @Val Claim claim,
            PeriodRepository periods, SheetRepository sheets)
            throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (!ClaimStatus.OPEN.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be deferred");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(until == null ? "" : until.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Not a date: " + until);
        }
        if (!date.isAfter(sheets.today(claim.getSheetId()))) {
            throw new InvalidClaimException("A claim can only be deferred to a date after the sheet's today");
        }
        claim.defer(date.toString());
    }
}
