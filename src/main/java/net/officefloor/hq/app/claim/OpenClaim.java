package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Opens a claim, giving it the sheet's next number and recording who opened it. */
public class OpenClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            ClaimRepository claims) throws ClaimStatusException {
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.OPEN).name());
        claim.setNumber(claims.nextNumber(claim.getSheetId()));
        claim.setOpenedBy(by == null ? "" : by);
    }
}
