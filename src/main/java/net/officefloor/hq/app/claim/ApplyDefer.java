package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Defers an OPEN claim until a date after the sheet's today; any other status, or a date not after today, is refused. */
public class ApplyDefer {
    public void apply(@Val Claim claim, @Val DeferUntil until, ClaimRepository repository, SheetRepository sheets)
            throws BadRequestException {
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new BadRequestException("Only an OPEN claim is deferred; this one is " + claim.getStatus());
        }
        if (!until.date().isAfter(sheets.today(claim.getSheetId()))) {
            throw new BadRequestException("A claim is deferred to a date after the sheet's today");
        }
        claim.setDeferredUntil(until.date());
        if (!repository.defer(claim)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
    }
}
