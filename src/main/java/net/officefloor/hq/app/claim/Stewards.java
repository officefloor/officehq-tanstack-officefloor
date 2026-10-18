package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.UserRole;
import net.officefloor.hq.app.setting.UserRoleRepository;

/** Refuses a user who is not a steward on the claim's sheet. */
final class Stewards {
    private Stewards() {
    }

    static void require(Claim claim, String by, UserRoleRepository roles, String action) throws NotStewardException {
        if (!UserRole.STEWARD.equals(roles.roleOf(claim.getSheetId(), by))) {
            throw new NotStewardException("Only a steward can " + action + " a pending claim.");
        }
    }
}
