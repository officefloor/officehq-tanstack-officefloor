package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a claim of the sheet by id. */
public class LoadClaim {
    public void load(@Val Sheet sheet, @PathVariable("claimId") String claimId, ClaimRepository repository,
            Out<Claim> claimOut) throws ClaimNotFoundException {
        Long id;
        try {
            id = Long.valueOf(claimId.trim());
        } catch (NumberFormatException e) {
            throw new ClaimNotFoundException(claimId);
        }
        claimOut.set(repository.findByIdAndSheetId(id, sheet.getId())
                .orElseThrow(() -> new ClaimNotFoundException(claimId)));
    }
}
