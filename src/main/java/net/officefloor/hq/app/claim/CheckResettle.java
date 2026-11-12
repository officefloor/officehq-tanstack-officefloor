package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a re-settle by anyone but a steward, or of a claim that is not SETTLED. */
public class CheckResettle {
    public void check(@Val ResettleInput input, @Val Claim claim, RoleStore roles) throws InvalidSheetException {
        if (roles.find(claim.getSheetId(), input.user()) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can re-settle a claim");
        }
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            throw new InvalidSheetException("Only a SETTLED claim can be re-settled");
        }
    }
}
