package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Set the loaded claim's due-by directly: only a steward may, and only on a DRAFT, OPEN or PENDING claim. The due-by
 *  then stays as set (a holiday added later does not move it); its overdue, late charge, grace and period follow it.
 *  Its number and any figure a statement bound it to stay as they are. A set another user's change got to first is
 *  refused. */
public class SaveDueBySet {
    public void save(@Val DueBySetInput input, @Val Claim loaded, ClaimRepository repository, RoleRepository roles,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Only a steward may set a claim's due-by.");
        }
        ClaimStatus status = loaded.status();
        if (status != ClaimStatus.DRAFT && status != ClaimStatus.OPEN && status != ClaimStatus.PENDING) {
            throw new ClaimStatusNotAllowedException("A " + status
                    + " claim's due-by cannot be set; only a DRAFT, OPEN or PENDING one's can.");
        }
        if (!repository.setDueBy(loaded, input.dueBy())) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(repository.find(loaded.sheetId(), loaded.id())));
    }
}
