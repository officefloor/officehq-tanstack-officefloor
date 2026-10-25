package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@Val SheetResponse sheet, @PathVariable("claimId") String claimId,
            ClaimRepository repository, Out<Claim> claim) throws BadRequestException {
        Claim found = null;
        try {
            found = repository.find(sheet.id(), Long.parseLong(claimId));
        } catch (NumberFormatException e) {
            // not an id: no such claim
        }
        if (found == null) {
            throw new BadRequestException("No such claim on this sheet");
        }
        claim.set(found);
    }
}
