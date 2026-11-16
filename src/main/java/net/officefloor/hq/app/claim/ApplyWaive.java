package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;

/** Checks a waiver: only a steward waives, only an OPEN or PENDING claim, and only one not already waived. */
public class ApplyWaive {
    public void apply(@Val Claim claim, @Val ClaimWaiver waiver, RoleRepository roles, ClaimRepository claims)
            throws InvalidClaimStatusException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(claim.sheetId(), waiver.by()))) {
            throw new InvalidClaimStatusException("Only a steward waives a late charge");
        }
        if (!ClaimDue.isOpen(claim)) {
            throw new InvalidClaimStatusException("Only an OPEN or PENDING claim's late charge can be waived");
        }
        if (claims.waivedOn(claim.sheetId(), claim.id()) != null) {
            throw new InvalidClaimStatusException("The claim's late charge is already waived");
        }
    }
}
