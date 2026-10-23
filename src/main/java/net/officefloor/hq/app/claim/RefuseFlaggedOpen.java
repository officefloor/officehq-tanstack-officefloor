package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.flag.FlagRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse opening a claim of a person flagged in force on the sheet's today (today on or before the flag's until
 *  date) unless the user acts as a steward. Shared by a single open, the bulk open and a paste. */
public class RefuseFlaggedOpen {
    public void refuse(@Val ClaimStatusInput input, @Val Claim loaded, RoleRepository roles, FlagRepository flags)
            throws ClaimStatusNotAllowedException {
        if (input.action().equals("open")) {
            refuse(loaded, input.user(), roles, flags);
        }
    }

    /** Throws when the user may not open the claim because its person is flagged. */
    static void refuse(Claim claim, String user, RoleRepository roles, FlagRepository flags)
            throws ClaimStatusNotAllowedException {
        String reason = flags.inForce(claim.sheetId(), claim.personId());
        if (reason != null && !Role.STEWARD.equals(roles.of(claim.sheetId(), user))) {
            throw new ClaimStatusNotAllowedException("This person is flagged (" + reason
                    + "); only a steward can open their claims.");
        }
    }
}
