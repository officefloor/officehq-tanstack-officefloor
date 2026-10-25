package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim on the sheet's today; any other status, or a second reminder that day, is refused. */
public class ApplyRemind {
    public void apply(@Val Claim claim, ClaimRepository repository, SheetRepository sheets) throws BadRequestException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new BadRequestException("Only an OPEN or PENDING claim is reminded; this one is " + claim.getStatus());
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.equals(claim.getRemindedOn())) {
            throw new BadRequestException("The claim was already reminded on " + today);
        }
        if (!repository.remind(claim, today)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
    }
}
