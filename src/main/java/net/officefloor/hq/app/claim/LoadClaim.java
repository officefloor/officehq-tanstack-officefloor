package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            ClaimRepository repository, Out<Claim> claimOut) throws ClaimNotFoundException {
        claimOut.set(repository.findByIdAndSheetId(claimId, sheetId.trim())
                .orElseThrow(() -> new ClaimNotFoundException("No such claim on this sheet.")));
    }
}
