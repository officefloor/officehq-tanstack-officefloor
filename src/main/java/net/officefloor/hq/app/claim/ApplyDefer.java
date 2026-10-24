package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Defers an OPEN claim until a date after the sheet's today. A claim in any other status, a date not after the sheet's
 * today, or a claim whose due-by is in a closed period is refused. Returns the date, for the audit.
 */
public class ApplyDefer {
    public LocalDate apply(@Val Claim claim, @Val Sheet sheet, ClaimRepository repository, PeriodRepository periods,
            @RequestParam(name = "until", required = false) String untilParam) throws ClaimStatusException {
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new ClaimStatusException("A claim in a closed period cannot be deferred.");
        }
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be deferred.");
        }
        LocalDate until;
        try {
            until = LocalDate.parse(untilParam == null ? "" : untilParam.trim());
        } catch (DateTimeParseException e) {
            throw new ClaimStatusException("Not a date: " + untilParam);
        }
        if (!until.isAfter(sheet.getToday())) {
            throw new ClaimStatusException("A claim can only be deferred to a date after the sheet's today.");
        }
        claim.setDeferredUntil(until);
        repository.saveAndFlush(claim);
        return until;
    }
}
